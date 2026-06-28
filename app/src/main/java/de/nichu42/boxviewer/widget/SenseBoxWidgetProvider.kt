package de.nichu42.boxviewer.widget

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
import de.nichu42.boxviewer.R
import de.nichu42.boxviewer.data.db.SenseBoxDatabase
import de.nichu42.boxviewer.data.repository.SenseBoxRepository
import de.nichu42.boxviewer.MainActivity
import de.nichu42.boxviewer.ui.WidgetConfigActivity
import de.nichu42.boxviewer.util.SensorDisplayConverter
import de.nichu42.boxviewer.util.SensorValueColorResolver
import de.nichu42.boxviewer.util.AqiSystem
import de.nichu42.boxviewer.util.AqiCalculator
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Display representation used only inside the widget. Keeps the AQI label separate from
 * the physical sensor unit so that [sensorUnit] is always a real unit (or null for AQI).
 */
private data class WidgetSensorDisplay(
    val sensorId: String,
    val sensorTitle: String,
    val sensorUnit: String?,
    val sensorType: String?,
    val value: String?,
    val aqiLabel: String? = null
)

open class SenseBoxWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Build the update stream for each widget on bootstrap
        for (appWidgetId in appWidgetIds) {
            updateWidgetAsync(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidgetAsync(context, appWidgetManager, appWidgetId)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val db = SenseBoxDatabase.getDatabase(context)
        val repository = SenseBoxRepository(context, db)
        CoroutineScope(Dispatchers.IO).launch {
            for (appWidgetId in appWidgetIds) {
                repository.deleteWidgetConfig(appWidgetId)
                cancelAlarm(context, appWidgetId, this@SenseBoxWidgetProvider.javaClass)
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
            // ACTION_SCREEN_ON cannot be delivered to manifest-declared receivers; only
            // ACTION_USER_PRESENT (device unlocked) reliably triggers here.
            Intent.ACTION_USER_PRESENT -> {
                updateAllWidgets(context, force = true)
            }
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                // AlarmManager alarms don't survive reboots or APK updates.
                // Re-schedule every widget's repeating alarm from its saved config,
                // then update the UI immediately from cache (no network hit on boot).
                rescheduleAllAlarms(context)
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "de.nichu42.boxviewer.widget.ACTION_REFRESH_WIDGET"

        private fun isLightColor(color: Int): Boolean {
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            val luminance = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0
            return luminance > 0.5
        }

        private fun getProviderClassForWidget(context: Context, appWidgetId: Int): Class<*> {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val info = appWidgetManager.getAppWidgetInfo(appWidgetId)
            return if (info != null) {
                try {
                    Class.forName(info.provider.className)
                } catch (e: Exception) {
                    SenseBoxWidgetProvider::class.java
                }
            } else {
                SenseBoxWidgetProvider::class.java
            }
        }

        fun scheduleAlarm(context: Context, appWidgetId: Int, intervalMinutes: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val providerClass = getProviderClassForWidget(context, appWidgetId)
            val intent = Intent(context, providerClass).apply {
                action = ACTION_REFRESH_WIDGET
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val intervalMs = intervalMinutes * 60 * 1000L
            val triggerTime = SystemClock.elapsedRealtime() + intervalMs
            alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME,
                triggerTime,
                intervalMs,
                pendingIntent
            )
        }

        fun cancelAlarm(context: Context, appWidgetId: Int, providerClass: Class<*>? = null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val targetClass = providerClass ?: getProviderClassForWidget(context, appWidgetId)
            // Intent must exactly match the one created in scheduleAlarm (same action + extras)
            // so that FLAG_NO_CREATE can locate the existing PendingIntent.
            val intent = Intent(context, targetClass).apply {
                action = ACTION_REFRESH_WIDGET
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
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
            val repository = SenseBoxRepository(context, db)
            CoroutineScope(Dispatchers.IO).launch {
                val config = repository.getWidgetConfig(appWidgetId) ?: return@launch
                val cachedSensors = repository.getCachedSensors(config.boxId)

                val views = buildRemoteViews(context, config, cachedSensors)
                appWidgetManager.updateAppWidget(appWidgetId, views)
                
                // Ensure repeating auto-update alarm is scheduled matching config
                scheduleAlarm(context, appWidgetId, config.refreshIntervalMinutes)
            }
        }

        fun rescheduleAllAlarms(context: Context) {
            val db = SenseBoxDatabase.getDatabase(context)
            val repository = SenseBoxRepository(context, db)
            val appWidgetManager = AppWidgetManager.getInstance(context)
            
            val providers = listOf(
                SenseBoxWidgetProvider::class.java,
                SenseBoxWidgetProviderSmall::class.java,
                SenseBoxWidgetProviderLarge::class.java
            )
            val activeWidgetIds = providers.flatMap { provider ->
                appWidgetManager.getAppWidgetIds(ComponentName(context, provider))?.toList() ?: emptyList()
            }.toSet()

            CoroutineScope(Dispatchers.IO).launch {
                val allConfigs = repository.getAllWidgetConfigs()
                for (config in allConfigs) {
                    if (config.widgetId in activeWidgetIds) {
                        // Widget is still on the home screen — reschedule its alarm and redraw from cache.
                        scheduleAlarm(context, config.widgetId, config.refreshIntervalMinutes)
                        updateWidgetAsync(context, appWidgetManager, config.widgetId)
                    } else {
                        // Config exists in DB but widget is no longer active — clean up the orphan.
                        repository.deleteWidgetConfig(config.widgetId)
                    }
                }
            }
        }

        fun updateAllWidgets(context: Context, force: Boolean = false) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val providers = listOf(
                SenseBoxWidgetProvider::class.java,
                SenseBoxWidgetProviderSmall::class.java,
                SenseBoxWidgetProviderLarge::class.java
            )
            for (provider in providers) {
                val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, provider))
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    for (appWidgetId in appWidgetIds) {
                        updateWidgetByFetchingAsync(context, appWidgetManager, appWidgetId, force = force)
                    }
                }
            }
        }

        fun updateWidgetByFetchingAsync(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, force: Boolean = false) {
            val db = SenseBoxDatabase.getDatabase(context)
            val repository = SenseBoxRepository(context, db)
            CoroutineScope(Dispatchers.IO).launch {
                val config = repository.getWidgetConfig(appWidgetId) ?: run {
                    // No config means this widget was deleted while the old buggy cancelAlarm
                    // left the alarm behind. Cancel it now so the ghost alarm self-destructs.
                    cancelAlarm(context, appWidgetId)
                    return@launch
                }

                // Keep things battery friendly: skip network fetches when the screen is off (non-interactive).
                // Wake-time forced refreshes (force = true) bypass this check so they don't silently skip
                // on devices where isInteractive lags behind ACTION_SCREEN_ON / ACTION_USER_PRESENT.
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                if (powerManager != null && !powerManager.isInteractive && !force) {
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
                    val views = buildRemoteViews(context, config, cachedSensors)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    return@launch
                }

                // Show spinner/loader immediately
                val startCachedSensors = repository.getCachedSensors(config.boxId)
                val loadingViews = buildRemoteViews(context, config, startCachedSensors, isLoading = true)
                appWidgetManager.updateAppWidget(appWidgetId, loadingViews)

                try {
                    // Fetch latest sensor values
                    repository.fetchAndSyncBox(config.boxId)
                    
                    // Update configuration timestamp in local store
                    val updatedConfig = config.copy(lastFetchedTime = System.currentTimeMillis())
                    repository.saveWidgetConfig(updatedConfig)

                    val cachedSensors = repository.getCachedSensors(config.boxId)

                    val views = buildRemoteViews(context, updatedConfig, cachedSensors, isLoading = false)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Re-draw in case network fails with what is cached
                    val cachedSensors = repository.getCachedSensors(config.boxId)
                    val views = buildRemoteViews(context, config, cachedSensors, isLoading = false)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }

        private fun buildRemoteViews(
            context: Context,
            config: de.nichu42.boxviewer.data.db.WidgetConfigEntity,
            sensors: List<de.nichu42.boxviewer.data.db.SensorCacheEntity>,
            isLoading: Boolean = false
        ): RemoteViews {
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val oldFahrenheit = prefs.getBoolean("use_fahrenheit", false)
            val defaultTemp = if (oldFahrenheit) "°F" else "°C"
            val temperatureUnit = prefs.getString("temperature_unit", defaultTemp) ?: defaultTemp
            val pressureUnit = prefs.getString("pressure_unit", "hPa") ?: "hPa"
            val windUnit = prefs.getString("wind_unit", "m/s") ?: "m/s"
            val formatPressure = prefs.getBoolean("format_pressure", true)
            val aqiStr = prefs.getString("aqi_system", AqiSystem.US_EPA.name) ?: AqiSystem.US_EPA.name
            val aqiSystem = try { AqiSystem.valueOf(aqiStr) } catch (e: Exception) { AqiSystem.US_EPA }

            // Synthesize the virtual AQI sensor and map to a widget display wrapper.
            // The wrapper keeps the AQI label out of sensorUnit.
            val displaySensors = AqiCalculator.synthesizeVirtualSensors(sensors, aqiSystem, config.boxId).map {
                if (it.sensorId == "virtual_aqi") {
                    WidgetSensorDisplay(
                        sensorId = it.sensorId,
                        sensorTitle = it.sensorTitle,
                        sensorUnit = null,
                        sensorType = it.sensorType,
                        value = it.value,
                        aqiLabel = it.sensorUnit // AqiCalculator stores the label here for the virtual sensor
                    )
                } else {
                    WidgetSensorDisplay(
                        sensorId = it.sensorId,
                        sensorTitle = it.sensorTitle,
                        sensorUnit = it.sensorUnit,
                        sensorType = it.sensorType,
                        value = it.value,
                        aqiLabel = null
                    )
                }
            }

            // Retrieve options for responsive size adaptations
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val options = appWidgetManager.getAppWidgetOptions(config.widgetId)
            val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 0
            val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) ?: 0

            val actualIsMetricMode = if (minHeight in 1..74) {
                // Too short to display a list, dynamically adapt by forcing metric highlight
                true
            } else {
                config.visualizationType == "GRID"
            }

            val layoutId = if (actualIsMetricMode) R.layout.widget_layout_metric else R.layout.widget_layout_list
            val views = RemoteViews(context.packageName, layoutId)

            // Setup box details with size-dependent visibility constraints
            val showBoxName = config.showBoxName && (minWidth == 0 || minWidth >= 120)
            val showUpdateTime = config.showUpdateTime && (minWidth == 0 || minWidth >= 160)
            val showRefreshButton = config.showRefreshButton && (minWidth == 0 || minWidth >= 140)
            val showConfigButton = config.showConfigButton && (minWidth == 0 || minWidth >= 140)

            views.setTextViewText(R.id.widget_box_name, config.boxName)
            views.setViewVisibility(R.id.widget_box_name, if (showBoxName) View.VISIBLE else View.GONE)

            // Format date string
            val updatedString = if (config.lastFetchedTime > 0) {
                val df = SimpleDateFormat("HH:mm", Locale.getDefault())
                "Updated ${df.format(Date(config.lastFetchedTime))}"
            } else {
                "Updated --:--"
            }
            views.setTextViewText(R.id.widget_update_time, updatedString)
            views.setViewVisibility(R.id.widget_update_time, if (showUpdateTime) View.VISIBLE else View.GONE)

            val textScale = config.textScale
            views.setTextViewTextSize(R.id.widget_box_name, android.util.TypedValue.COMPLEX_UNIT_SP, 13f * textScale)
            views.setTextViewTextSize(R.id.widget_update_time, android.util.TypedValue.COMPLEX_UNIT_SP, 9f * textScale)

            if (isLoading) {
                views.setViewVisibility(R.id.widget_refresh_button, View.GONE)
                views.setViewVisibility(R.id.widget_loading_spinner, if (minWidth == 0 || minWidth >= 140) View.VISIBLE else View.GONE)
            } else {
                views.setViewVisibility(R.id.widget_refresh_button, if (showRefreshButton) View.VISIBLE else View.GONE)
                views.setViewVisibility(R.id.widget_loading_spinner, View.GONE)
            }
            views.setViewVisibility(R.id.widget_settings_button, if (showConfigButton) View.VISIBLE else View.GONE)

            // Theme indices background colors (modern, material palettes).
            // Handles backward-compatibility for values < 10, or treats them as ARGB colors directly.
            val themeColor = if (config.themeColorIndex in 0..9) {
                val presets = listOf(
                    0xFF0F172A.toInt(), // Slate Dark
                    0xFF020617.toInt(), // Deep Navy
                    0xFF064E3B.toInt(), // Forest Green
                    0xFF0F3D5C.toInt(), // Celestial Blue
                    0xFF581C87.toInt(), // Royal Purple
                    0xFFF8FAFC.toInt(), // Slate Light
                    0xFFECFDF5.toInt(), // Mint Green Light
                    0xFFF0F9FF.toInt(), // Sky Blue Light
                    0xFFFFFBEB.toInt(), // Warm Cream Light
                    0xFF18181B.toInt()  // Dark Charcoal
                )
                presets.getOrElse(config.themeColorIndex) { 0xFF0F172A.toInt() }
            } else {
                config.themeColorIndex
            }
            views.setInt(R.id.widget_root, "setBackgroundColor", themeColor)

            // Dynamic text/icon contrast styling based on background luminance
            val isLight = isLightColor(themeColor)
            val textColor = if (isLight) 0xFF0F172A.toInt() else 0xFFF8FAFC.toInt()
            val subTextColor = if (isLight) 0xFF475569.toInt() else 0xFF94A3B8.toInt()
            val iconColor = if (isLight) 0xFF0F172A.toInt() else 0xFFF8FAFC.toInt()

            // Check if outer background is fully transparent (alpha == 0)
            val alpha = (themeColor shr 24) and 0xFF
            val innerBgOverlay = if (alpha == 0) {
                0x00000000
            } else if (isLight) {
                0x0A000000
            } else {
                0x1AFFFFFF
            }

            // Apply global colors to header
            views.setTextColor(R.id.widget_box_name, textColor)
            views.setTextColor(R.id.widget_update_time, subTextColor)
            views.setInt(R.id.widget_refresh_button, "setColorFilter", iconColor)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                views.setColorStateList(
                    R.id.widget_loading_spinner,
                    "setIndeterminateTintList",
                    android.content.res.ColorStateList.valueOf(iconColor)
                )
            }
            views.setInt(R.id.widget_settings_button, "setColorFilter", iconColor)
            views.setInt(R.id.widget_values_container, "setBackgroundColor", innerBgOverlay)

            // Split dynamic config sensor elements
            val selectedIdsList = config.sensorIdsString.split(",").filter { it.isNotEmpty() }

            if (actualIsMetricMode) {
                val targetSensor = if (selectedIdsList.isNotEmpty()) {
                    displaySensors.find { it.sensorId == selectedIdsList.first() } ?: displaySensors.firstOrNull()
                } else {
                    displaySensors.firstOrNull()
                }

                views.setTextViewTextSize(R.id.big_sensor_value, android.util.TypedValue.COMPLEX_UNIT_SP, 26f * textScale)
                views.setTextViewTextSize(R.id.big_sensor_title, android.util.TypedValue.COMPLEX_UNIT_SP, 11f * textScale)

                if (targetSensor != null) {
                    val showLabel = config.metricDisplayMode == "LABEL_VALUE_UNIT"
                    val showUnit = config.metricDisplayMode == "LABEL_VALUE_UNIT" || config.metricDisplayMode == "VALUE_UNIT"
                    
                    val conversion = SensorDisplayConverter.convert(
                        rawValue = targetSensor.value,
                        sourceUnit = targetSensor.sensorUnit,
                        temperatureUnit = temperatureUnit,
                        pressureUnit = pressureUnit,
                        windUnit = windUnit,
                        formatPressure = formatPressure
                    )
                    val displayVal = conversion.value
                    val displayUnit = conversion.unit
                    val valueText = if (targetSensor.sensorId == "virtual_aqi") {
                        // AQI: format according to aqiDisplayMode; aqiLabel holds the AQI label text
                        val aqiNumber = targetSensor.value ?: "--"
                        val aqiLabel = targetSensor.aqiLabel ?: ""
                        when (config.aqiDisplayMode) {
                            "NUMBER_ONLY" -> aqiNumber
                            "LABEL_ONLY"  -> aqiLabel.ifEmpty { aqiNumber }
                            else          -> if (aqiLabel.isNotEmpty()) "$aqiNumber\n$aqiLabel" else aqiNumber
                        }
                    } else if (showUnit) {
                        "$displayVal $displayUnit"
                    } else {
                        displayVal
                    }
                    views.setTextViewText(R.id.big_sensor_value, valueText)
                    
                    if (showLabel) {
                        views.setViewVisibility(R.id.big_sensor_title, View.VISIBLE)
                        views.setTextViewText(R.id.big_sensor_title, targetSensor.sensorTitle)
                        views.setTextColor(R.id.big_sensor_title, subTextColor)
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
                    
                    val valueColor = if (config.useConditionalFormatting) {
                        // Use the converted display value so thresholds are applied to canonical units.
                        val colorInput = if (targetSensor.sensorId == "virtual_aqi") targetSensor.value else displayVal
                        getSensorValueColor(targetSensor.sensorTitle, colorInput, visuals.second, aqiSystem)
                    } else {
                        visuals.second
                    }
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
                val maxRows = if (minHeight > 0) {
                    when {
                        minHeight < 120 -> 3
                        minHeight < 180 -> 4
                        else -> 6
                    }
                } else {
                    6
                }

                val listToDisplay = if (selectedIdsList.isNotEmpty()) {
                    val matched = mutableListOf<WidgetSensorDisplay>()
                    for (selectedId in selectedIdsList) {
                        displaySensors.find { it.sensorId == selectedId }?.let { matched.add(it) }
                    }
                    matched.take(maxRows)
                } else {
                    displaySensors.take(maxRows)
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
                    if (sensor != null && i < maxRows) {
                        views.setViewVisibility(rows[i], View.VISIBLE)
                        
                        val showLabel = config.metricDisplayMode == "LABEL_VALUE_UNIT"
                        val showUnit = config.metricDisplayMode == "LABEL_VALUE_UNIT" || config.metricDisplayMode == "VALUE_UNIT"
                        
                        if (showLabel) {
                            views.setViewVisibility(titles[i], View.VISIBLE)
                            views.setTextViewText(titles[i], sensor.sensorTitle)
                            views.setTextColor(titles[i], subTextColor)
                        } else {
                            views.setViewVisibility(titles[i], View.GONE)
                        }
                        
                        val conversion = SensorDisplayConverter.convert(
                            rawValue = sensor.value,
                            sourceUnit = sensor.sensorUnit,
                            temperatureUnit = temperatureUnit,
                            pressureUnit = pressureUnit,
                            windUnit = windUnit,
                            formatPressure = formatPressure
                        )
                        val displayVal = conversion.value
                        val displayUnit = conversion.unit
                        val valStr = if (sensor.sensorId == "virtual_aqi") {
                            val aqiNumber = sensor.value ?: "--"
                            val aqiLabel = sensor.aqiLabel ?: ""
                            when (config.aqiDisplayMode) {
                                "NUMBER_ONLY" -> aqiNumber
                                "LABEL_ONLY"  -> aqiLabel.ifEmpty { aqiNumber }
                                else          -> if (aqiLabel.isNotEmpty()) "$aqiNumber $aqiLabel" else aqiNumber
                            }
                        } else if (showUnit) {
                            "$displayVal $displayUnit"
                        } else {
                            displayVal
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
                        
                        val valueColor = if (config.useConditionalFormatting) {
                            // Use the converted display value so thresholds are applied to canonical units.
                            val colorInput = if (sensor.sensorId == "virtual_aqi") sensor.value else displayVal
                            getSensorValueColor(sensor.sensorTitle, colorInput, visuals.second, aqiSystem)
                        } else {
                            visuals.second
                        }
                        views.setTextColor(vals[i], valueColor)
                    } else {
                        views.setViewVisibility(rows[i], View.GONE)
                    }
                }
            }

            val appIntent = Intent(context, MainActivity::class.java).apply {
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
            val configIntent = Intent(context, WidgetConfigActivity::class.java).apply {
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
                lower.contains("aqi") || lower.contains("air quality index") -> Pair(R.drawable.ic_sensor_air, 0xFF0D9488.toInt()) // Teal – AQI virtual sensor
                lower.contains("pm10") || lower.contains("pm2") || lower.contains("air") || lower.contains("dust") || lower.contains("feinstaub") -> Pair(R.drawable.ic_sensor_air, 0xFFEC4899.toInt()) // Pink
                lower.contains("bell") || lower.contains("light") || lower.contains("lux") || lower.contains("sonne") || lower.contains("uv") -> Pair(R.drawable.ic_sensor_light, 0xFFF59E0B.toInt()) // Amber/Yellow
                else -> Pair(R.drawable.ic_sensor_generic, 0xFF10B981.toInt()) // Emerald Green
            }
        }

        private fun getSensorValueColor(
            title: String,
            valueString: String?,
            defaultColor: Int,
            aqiSystem: AqiSystem
        ): Int = SensorValueColorResolver.resolveColor(
            title = title,
            valueString = valueString,
            unit = null, // widget callers already convert to canonical units before passing
            aqiSystem = aqiSystem,
            defaultColor = defaultColor
        )
    }
}
