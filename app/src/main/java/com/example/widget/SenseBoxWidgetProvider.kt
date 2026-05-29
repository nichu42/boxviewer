package com.example.widget

import android.content.ComponentName
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.example.R
import com.example.data.db.SenseBoxDatabase
import com.example.data.repository.SenseBoxRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SenseBoxWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Build the update stream for each widget on bootstrap
        for (appWidgetId in appWidgetIds) {
            updateWidgetAsync(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val db = SenseBoxDatabase.getDatabase(context)
        val repository = SenseBoxRepository(db)
        CoroutineScope(Dispatchers.IO).launch {
            for (appWidgetId in appWidgetIds) {
                repository.deleteWidgetConfig(appWidgetId)
                cancelAlarm(context, appWidgetId)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH_WIDGET -> {
                val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                val force = intent.getBooleanExtra("force", false)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    updateWidgetByFetchingAsync(context, appWidgetManager, appWidgetId, force = force)
                }
            }
            Intent.ACTION_USER_PRESENT, Intent.ACTION_SCREEN_ON -> {
                updateAllWidgets(context, force = true)
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.example.widget.ACTION_REFRESH_WIDGET"

        fun scheduleAlarm(context: Context, appWidgetId: Int, intervalMinutes: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, SenseBoxWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = SystemClock.elapsedRealtime() + intervalMinutes * 60 * 1000
            alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME,
                triggerTime,
                intervalMinutes * 60 * 1000L,
                pendingIntent
            )
        }

        fun cancelAlarm(context: Context, appWidgetId: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, SenseBoxWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }

        fun updateWidgetAsync(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val db = SenseBoxDatabase.getDatabase(context)
            val repository = SenseBoxRepository(db)
            CoroutineScope(Dispatchers.IO).launch {
                val config = repository.getWidgetConfig(appWidgetId) ?: return@launch
                val cachedSensors = repository.getCachedSensors(config.boxId)
                val box = repository.getSavedBox(config.boxId)

                val views = buildRemoteViews(context, config, cachedSensors, box?.exposure)
                appWidgetManager.updateAppWidget(appWidgetId, views)
                
                // Ensure repeating auto-update alarm is scheduled matching config
                scheduleAlarm(context, appWidgetId, config.refreshIntervalMinutes)
            }
        }

        fun updateAllWidgets(context: Context, force: Boolean = false) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, SenseBoxWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                for (appWidgetId in appWidgetIds) {
                    updateWidgetByFetchingAsync(context, appWidgetManager, appWidgetId, force = force)
                }
            }
        }

        fun updateWidgetByFetchingAsync(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, force: Boolean = false) {
            val db = SenseBoxDatabase.getDatabase(context)
            val repository = SenseBoxRepository(db)
            CoroutineScope(Dispatchers.IO).launch {
                val config = repository.getWidgetConfig(appWidgetId) ?: return@launch
                
                // Keep things battery friendly: skip network fetches when the screen is off (non-interactive).
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                if (powerManager != null && !powerManager.isInteractive) {
                    return@launch
                }

                val lastFetched = config.lastFetchedTime
                val now = System.currentTimeMillis()
                // Throttle automatic refreshes to at least 5 minutes to protect API and conserve battery.
                // Manual updates (force = true) bypass this check.
                val shouldFetch = force || (now - lastFetched >= 5 * 60 * 1000L)

                if (!shouldFetch) {
                    // Update the widget UI immediately using cached values, avoiding any network calls or background tasks
                    val cachedSensors = repository.getCachedSensors(config.boxId)
                    val box = repository.getSavedBox(config.boxId)
                    val views = buildRemoteViews(context, config, cachedSensors, box?.exposure)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    return@launch
                }

                try {
                    // Fetch latest sensor values
                    repository.fetchAndSyncBox(config.boxId)
                    
                    // Update configuration timestamp in local store
                    val updatedConfig = config.copy(lastFetchedTime = System.currentTimeMillis())
                    repository.saveWidgetConfig(updatedConfig)

                    val cachedSensors = repository.getCachedSensors(config.boxId)
                    val box = repository.getSavedBox(config.boxId)

                    val views = buildRemoteViews(context, updatedConfig, cachedSensors, box?.exposure)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Re-draw in case network fails with what is cached
                    val cachedSensors = repository.getCachedSensors(config.boxId)
                    val box = repository.getSavedBox(config.boxId)
                    val views = buildRemoteViews(context, config, cachedSensors, box?.exposure)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }

        private fun buildRemoteViews(
            context: Context,
            config: com.example.data.db.WidgetConfigEntity,
            sensors: List<com.example.data.db.SensorCacheEntity>,
            exposure: String?
        ): RemoteViews {
            val isMetricMode = config.visualizationType == "GRID" // LIST or GRID (Metric)
            val layoutId = if (isMetricMode) R.layout.widget_layout_metric else R.layout.widget_layout_list
            val views = RemoteViews(context.packageName, layoutId)

            // Setup box details
            views.setTextViewText(R.id.widget_box_name, config.boxName)
            views.setViewVisibility(R.id.widget_box_name, if (config.showBoxName) View.VISIBLE else View.GONE)

            // Format date string
            val updatedString = if (config.lastFetchedTime > 0) {
                val df = SimpleDateFormat("HH:mm", Locale.getDefault())
                "Updated ${df.format(Date(config.lastFetchedTime))}"
            } else {
                "Updated --:--"
            }
            views.setTextViewText(R.id.widget_update_time, updatedString)
            views.setViewVisibility(R.id.widget_update_time, if (config.showUpdateTime) View.VISIBLE else View.GONE)

            val textScale = config.textScale
            views.setTextViewTextSize(R.id.widget_box_name, android.util.TypedValue.COMPLEX_UNIT_SP, 13f * textScale)
            views.setTextViewTextSize(R.id.widget_update_time, android.util.TypedValue.COMPLEX_UNIT_SP, 9f * textScale)

            views.setViewVisibility(R.id.widget_refresh_button, if (config.showRefreshButton) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widget_settings_button, if (config.showConfigButton) View.VISIBLE else View.GONE)

            // Theme indices background colors (modern, material palettes).
            // Handles backward-compatibility for values < 10, or treats them as ARGB colors directly.
            val themeColor = if (config.themeColorIndex in 0..9) {
                val presets = listOf(0xFF0F172A.toInt(), 0xFF064E3B.toInt(), 0xFF0F3D5C.toInt(), 0xFF581C87.toInt())
                presets.getOrElse(config.themeColorIndex) { 0xFF0F172A.toInt() }
            } else {
                config.themeColorIndex
            }
            views.setInt(R.id.widget_root, "setBackgroundColor", themeColor)

            // Split dynamic config sensor elements
            val selectedIdsList = config.sensorIdsString.split(",").filter { it.isNotEmpty() }

            if (isMetricMode) {
                val targetSensor = if (selectedIdsList.isNotEmpty()) {
                    sensors.find { it.sensorId == selectedIdsList.first() } ?: sensors.firstOrNull()
                } else {
                    sensors.firstOrNull()
                }

                views.setTextViewTextSize(R.id.big_sensor_value, android.util.TypedValue.COMPLEX_UNIT_SP, 26f * textScale)
                views.setTextViewTextSize(R.id.big_sensor_title, android.util.TypedValue.COMPLEX_UNIT_SP, 11f * textScale)

                if (targetSensor != null) {
                    val showLabel = config.metricDisplayMode == "LABEL_VALUE_UNIT"
                    val showUnit = config.metricDisplayMode == "LABEL_VALUE_UNIT" || config.metricDisplayMode == "VALUE_UNIT"
                    
                    val valueText = if (showUnit) {
                        "${targetSensor.value ?: "--"} ${targetSensor.sensorUnit ?: ""}"
                    } else {
                        targetSensor.value ?: "--"
                    }
                    views.setTextViewText(R.id.big_sensor_value, valueText)
                    
                    if (showLabel) {
                        views.setViewVisibility(R.id.big_sensor_title, View.VISIBLE)
                        views.setTextViewText(R.id.big_sensor_title, targetSensor.sensorTitle)
                    } else {
                        views.setViewVisibility(R.id.big_sensor_title, View.GONE)
                    }
                    
                    val visuals = getSensorVisuals(targetSensor.sensorTitle)
                    views.setImageViewResource(R.id.big_sensor_icon, visuals.first)
                    views.setInt(R.id.big_sensor_icon, "setColorFilter", visuals.second)
                    
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        views.setViewLayoutWidth(R.id.big_sensor_icon, 40f * textScale, android.util.TypedValue.COMPLEX_UNIT_DIP)
                        views.setViewLayoutHeight(R.id.big_sensor_icon, 40f * textScale, android.util.TypedValue.COMPLEX_UNIT_DIP)
                    }
                    
                    val valueColor = getSensorValueColor(targetSensor.sensorTitle, targetSensor.value, visuals.second)
                    views.setTextColor(R.id.big_sensor_value, valueColor)
                } else {
                    views.setTextViewText(R.id.big_sensor_value, "--")
                    views.setTextViewText(R.id.big_sensor_title, "No active sensor")
                    views.setImageViewResource(R.id.big_sensor_icon, R.drawable.ic_sensor_generic)
                    views.setInt(R.id.big_sensor_icon, "setColorFilter", 0xFF94A3B8.toInt())
                    
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        views.setViewLayoutWidth(R.id.big_sensor_icon, 40f * textScale, android.util.TypedValue.COMPLEX_UNIT_DIP)
                        views.setViewLayoutHeight(R.id.big_sensor_icon, 40f * textScale, android.util.TypedValue.COMPLEX_UNIT_DIP)
                    }
                    
                    views.setTextColor(R.id.big_sensor_value, 0xFF38BDF8.toInt())
                }
            } else {
                val listToDisplay = if (selectedIdsList.isNotEmpty()) {
                    val matched = mutableListOf<com.example.data.db.SensorCacheEntity>()
                    for (selectedId in selectedIdsList) {
                        sensors.find { it.sensorId == selectedId }?.let { matched.add(it) }
                    }
                    matched.take(6)
                } else {
                    sensors.take(6)
                }

                val titles = listOf(
                    R.id.sensor_title_1, R.id.sensor_title_2, R.id.sensor_title_3,
                    R.id.sensor_title_4, R.id.sensor_title_5, R.id.sensor_title_6
                )
                val vals = listOf(
                    R.id.sensor_value_1, R.id.sensor_value_2, R.id.sensor_value_3,
                    R.id.sensor_value_4, R.id.sensor_value_5, R.id.sensor_value_6
                )
                val icons = listOf(
                    R.id.sensor_icon_1, R.id.sensor_icon_2, R.id.sensor_icon_3,
                    R.id.sensor_icon_4, R.id.sensor_icon_5, R.id.sensor_icon_6
                )
                val rows = listOf(
                    R.id.row_1, R.id.row_2, R.id.row_3,
                    R.id.row_4, R.id.row_5, R.id.row_6
                )

                for (i in 0 until 6) {
                    val sensor = listToDisplay.getOrNull(i)
                    if (sensor != null) {
                        views.setViewVisibility(rows[i], View.VISIBLE)
                        
                        val showLabel = config.metricDisplayMode == "LABEL_VALUE_UNIT"
                        val showUnit = config.metricDisplayMode == "LABEL_VALUE_UNIT" || config.metricDisplayMode == "VALUE_UNIT"
                        
                        if (showLabel) {
                            views.setViewVisibility(titles[i], View.VISIBLE)
                            views.setTextViewText(titles[i], sensor.sensorTitle)
                        } else {
                            views.setViewVisibility(titles[i], View.GONE)
                        }
                        
                        val valStr = if (showUnit) {
                            "${sensor.value ?: "--"} ${sensor.sensorUnit ?: ""}"
                        } else {
                            sensor.value ?: "--"
                        }
                        views.setTextViewText(vals[i], valStr)

                        views.setTextViewTextSize(titles[i], android.util.TypedValue.COMPLEX_UNIT_SP, 11f * textScale)
                        views.setTextViewTextSize(vals[i], android.util.TypedValue.COMPLEX_UNIT_SP, 11f * textScale)

                        val visuals = getSensorVisuals(sensor.sensorTitle)
                        views.setImageViewResource(icons[i], visuals.first)
                        views.setInt(icons[i], "setColorFilter", visuals.second)
                        
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            views.setViewLayoutWidth(icons[i], 14f * textScale, android.util.TypedValue.COMPLEX_UNIT_DIP)
                            views.setViewLayoutHeight(icons[i], 14f * textScale, android.util.TypedValue.COMPLEX_UNIT_DIP)
                        }
                        
                        val valueColor = getSensorValueColor(sensor.sensorTitle, sensor.value, visuals.second)
                        views.setTextColor(vals[i], valueColor)
                    } else {
                        views.setViewVisibility(rows[i], View.GONE)
                    }
                }
            }

            val appIntent = Intent(context, com.example.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("box_id", config.boxId)
            }
            val pendingAppIntent = PendingIntent.getActivity(
                context,
                config.widgetId,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            // Tap on the name or the update time to open details
            views.setOnClickPendingIntent(R.id.widget_box_name, pendingAppIntent)
            views.setOnClickPendingIntent(R.id.widget_update_time, pendingAppIntent)

            // Dynamic gear icon settings configuration intent
            val configIntent = Intent(context, com.example.ui.WidgetConfigActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, config.widgetId)
            }
            val pendingConfigIntent = PendingIntent.getActivity(
                context,
                config.widgetId + 10000, // separate request code namespace
                configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_settings_button, pendingConfigIntent)

            // Manual sync/refresh button intent
            val refreshIntent = Intent(context, SenseBoxWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, config.widgetId)
                putExtra("force", true)
            }
            val pendingRefreshIntent = PendingIntent.getBroadcast(
                context,
                config.widgetId + 20000, // separate request code namespace
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh_button, pendingRefreshIntent)
            // Tapping on the values block also triggers auto-refresh
            views.setOnClickPendingIntent(R.id.widget_values_container, pendingRefreshIntent)

            return views
        }

        private fun getSensorVisuals(title: String): Pair<Int, Int> {
            val lower = title.lowercase()
            return when {
                lower.contains("temp") -> Pair(R.drawable.ic_sensor_temp, 0xFFF97316.toInt()) // Orange/Amber
                lower.contains("feucht") || lower.contains("humid") -> Pair(R.drawable.ic_sensor_humidity, 0xFF0EA5E9.toInt()) // Sky Blue/Teal
                lower.contains("druck") || lower.contains("press") -> Pair(R.drawable.ic_sensor_pressure, 0xFF8B5CF6.toInt()) // Purple
                lower.contains("pm10") || lower.contains("pm2") || lower.contains("air") || lower.contains("dust") || lower.contains("feinstaub") -> Pair(R.drawable.ic_sensor_air, 0xFFEC4899.toInt()) // Pink
                lower.contains("bell") || lower.contains("light") || lower.contains("lux") || lower.contains("sonne") || lower.contains("uv") -> Pair(R.drawable.ic_sensor_light, 0xFFF59E0B.toInt()) // Amber/Yellow
                else -> Pair(R.drawable.ic_sensor_generic, 0xFF10B981.toInt()) // Emerald Green
            }
        }

        private fun getSensorValueColor(title: String, valueString: String?, defaultColor: Int): Int {
            val value = valueString?.toDoubleOrNull() ?: return defaultColor
            val lower = title.lowercase()
            return when {
                lower.contains("temp") -> {
                    when {
                        value <= 0.0 -> 0xFF1E88E5.toInt()      // Freezing: Deep Blue
                        value <= 15.0 -> 0xFF64B5F6.toInt()     // Cold: Light Blue
                        value <= 25.0 -> 0xFF4CAF50.toInt()     // Comfortable: Green
                        value <= 32.0 -> 0xFFFFA726.toInt()     // Warm: Orange
                        else -> 0xFFE53935.toInt()              // Hot: Red
                    }
                }
                lower.contains("feucht") || lower.contains("humid") -> {
                    when {
                        value < 30.0 -> 0xFFFFB74D.toInt()      // Low/Dry: Amber
                        value <= 60.0 -> 0xFF4CAF50.toInt()     // Optimal: Green
                        else -> 0xFF0288D1.toInt()              // High/Sticky: Moisture Blue
                    }
                }
                lower.contains("druck") || lower.contains("press") -> {
                    when {
                        value < 1000.0 -> 0xFF90A4AE.toInt()    // Low/Stormy: Stormy Gray
                        value <= 1020.0 -> 0xFF4CAF50.toInt()   // Normal/Stable: Green
                        else -> 0xFFFBC02D.toInt()              // High/Dry: Bright Yellow
                    }
                }
                lower.contains("pm10") -> {
                    when {
                        value <= 45.0 -> 0xFF4CAF50.toInt()     // Good: Green
                        value <= 60.0 -> 0xFFFBC02D.toInt()     // Fair: Yellow
                        value <= 100.0 -> 0xFFFFA726.toInt()    // Poor: Orange
                        else -> 0xFFE53935.toInt()              // Severe: Red
                    }
                }
                lower.contains("pm2") || lower.contains("pm 2") -> {
                    when {
                        value <= 15.0 -> 0xFF4CAF50.toInt()     // Good: Green
                        value <= 25.0 -> 0xFFFBC02D.toInt()     // Fair: Yellow
                        value <= 50.0 -> 0xFFFFA726.toInt()     // Poor: Orange
                        else -> 0xFFE53935.toInt()              // Severe: Red
                    }
                }
                else -> defaultColor
            }
        }
    }
}

// Trigger CI Build
