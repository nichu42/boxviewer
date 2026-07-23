package de.nichu42.boxviewer.util

import android.content.Context
import android.net.Uri
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import de.nichu42.boxviewer.data.db.SavedBoxEntity
import de.nichu42.boxviewer.data.db.WidgetConfigEntity
import de.nichu42.boxviewer.data.repository.SenseBoxRepository
import de.nichu42.boxviewer.widget.SenseBoxWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@JsonClass(generateAdapter = true)
data class BackupData(
    val version: Int = 1,
    val appVersion: String = "",
    val exportedAt: String = "",
    val preferences: Map<String, String> = emptyMap(),
    val savedBoxes: List<BackupSavedBox> = emptyList(),
    val widgetConfigs: List<BackupWidgetConfig> = emptyList()
)

@JsonClass(generateAdapter = true)
data class BackupSavedBox(
    val boxId: String,
    val name: String,
    val description: String? = null,
    val exposure: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val savedAt: Long = System.currentTimeMillis(),
    val dashboardSensorIds: String? = null
)

@JsonClass(generateAdapter = true)
data class BackupWidgetConfig(
    val widgetId: Int,
    val boxId: String,
    val boxName: String = "",
    val sensorIdsString: String = "",
    val refreshIntervalMinutes: Int = 30,
    val visualizationType: String = "LIST",
    val themeColorIndex: Int = 0,
    val lastFetchedTime: Long = 0,
    val textScale: Float = 1.0f,
    val metricDisplayMode: String = "LABEL_VALUE_UNIT",
    val showRefreshButton: Boolean = true,
    val showConfigButton: Boolean = true,
    val showBoxName: Boolean = true,
    val showUpdateTime: Boolean = true,
    val useConditionalFormatting: Boolean = true,
    val aqiDisplayMode: String = "NUMBER_AND_LABEL"
)

data class BackupImportResult(
    val boxesRestored: Int,
    val widgetsRestored: Int,
    val preferencesRestored: Int
)

object BackupManager {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val backupAdapter = moshi.adapter(BackupData::class.java).indent("  ")

    fun generateBackupFilename(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        return "boxviewer-backup-$timestamp.json"
    }

    suspend fun createBackupData(context: Context, repository: SenseBoxRepository): BackupData = withContext(Dispatchers.IO) {
        val appVersion = try {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            pi.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }

        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val prefMap = mutableMapOf<String, String>()
        for ((key, value) in prefs.all) {
            if (value != null) {
                prefMap[key] = value.toString()
            }
        }

        val boxes = repository.getSavedBoxesList().map { entity ->
            BackupSavedBox(
                boxId = entity.boxId,
                name = entity.name,
                description = entity.description,
                exposure = entity.exposure,
                latitude = entity.latitude,
                longitude = entity.longitude,
                savedAt = entity.savedAt,
                dashboardSensorIds = entity.dashboardSensorIds
            )
        }

        val widgetConfigs = repository.getAllWidgetConfigs().map { entity ->
            BackupWidgetConfig(
                widgetId = entity.widgetId,
                boxId = entity.boxId,
                boxName = entity.boxName,
                sensorIdsString = entity.sensorIdsString,
                refreshIntervalMinutes = entity.refreshIntervalMinutes,
                visualizationType = entity.visualizationType,
                themeColorIndex = entity.themeColorIndex,
                lastFetchedTime = entity.lastFetchedTime,
                textScale = entity.textScale,
                metricDisplayMode = entity.metricDisplayMode,
                showRefreshButton = entity.showRefreshButton,
                showConfigButton = entity.showConfigButton,
                showBoxName = entity.showBoxName,
                showUpdateTime = entity.showUpdateTime,
                useConditionalFormatting = entity.useConditionalFormatting,
                aqiDisplayMode = entity.aqiDisplayMode
            )
        }

        val formattedDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

        BackupData(
            version = 1,
            appVersion = appVersion,
            exportedAt = formattedDate,
            preferences = prefMap,
            savedBoxes = boxes,
            widgetConfigs = widgetConfigs
        )
    }

    suspend fun exportBackupToUri(context: Context, repository: SenseBoxRepository, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val backupData = createBackupData(context, repository)
            val jsonString = backupAdapter.toJson(backupData)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
            } ?: return@withContext Result.failure(Exception("Failed to open output stream"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importBackupFromUri(context: Context, repository: SenseBoxRepository, uri: Uri): Result<BackupImportResult> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readText()
            } ?: return@withContext Result.failure(Exception("Failed to open input stream"))

            val backupData = backupAdapter.fromJson(jsonString)
                ?: return@withContext Result.failure(Exception("Invalid JSON format"))

            // 1. Restore Preferences
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            var prefsCount = 0
            for ((key, value) in backupData.preferences) {
                when {
                    value.equals("true", ignoreCase = true) || value.equals("false", ignoreCase = true) -> {
                        editor.putBoolean(key, value.toBoolean())
                    }
                    value.toIntOrNull() != null -> {
                        editor.putInt(key, value.toInt())
                    }
                    value.toFloatOrNull() != null && key == "app_text_scale" -> {
                        editor.putFloat(key, value.toFloat())
                    }
                    else -> {
                        editor.putString(key, value)
                    }
                }
                prefsCount++
            }
            editor.apply()

            // 2. Restore Saved Boxes
            var boxesCount = 0
            for (box in backupData.savedBoxes) {
                val entity = SavedBoxEntity(
                    boxId = box.boxId,
                    name = box.name,
                    description = box.description,
                    exposure = box.exposure,
                    latitude = box.latitude,
                    longitude = box.longitude,
                    savedAt = box.savedAt,
                    dashboardSensorIds = box.dashboardSensorIds
                )
                repository.saveSavedBox(entity)
                try {
                    repository.fetchAndSyncBox(box.boxId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                boxesCount++
            }

            // 3. Restore Widget Configs
            var widgetsCount = 0
            for (wConfig in backupData.widgetConfigs) {
                val entity = WidgetConfigEntity(
                    widgetId = wConfig.widgetId,
                    boxId = wConfig.boxId,
                    boxName = wConfig.boxName,
                    sensorIdsString = wConfig.sensorIdsString,
                    refreshIntervalMinutes = wConfig.refreshIntervalMinutes,
                    visualizationType = wConfig.visualizationType,
                    themeColorIndex = wConfig.themeColorIndex,
                    lastFetchedTime = wConfig.lastFetchedTime,
                    textScale = wConfig.textScale,
                    metricDisplayMode = wConfig.metricDisplayMode,
                    showRefreshButton = wConfig.showRefreshButton,
                    showConfigButton = wConfig.showConfigButton,
                    showBoxName = wConfig.showBoxName,
                    showUpdateTime = wConfig.showUpdateTime,
                    useConditionalFormatting = wConfig.useConditionalFormatting,
                    aqiDisplayMode = wConfig.aqiDisplayMode
                )
                repository.saveWidgetConfig(entity)
                widgetsCount++
            }

            // 4. Force update all home widgets
            SenseBoxWidgetProvider.updateAllWidgets(context, force = true)

            Result.success(BackupImportResult(boxesRestored = boxesCount, widgetsRestored = widgetsCount, preferencesRestored = prefsCount))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
