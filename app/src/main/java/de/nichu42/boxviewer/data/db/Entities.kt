package de.nichu42.boxviewer.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_boxes")
data class SavedBoxEntity(
    @PrimaryKey val boxId: String,
    val name: String,
    val description: String?,
    val exposure: String?,
    val latitude: Double,
    val longitude: Double,
    val savedAt: Long = System.currentTimeMillis(),
    val dashboardSensorIds: String? = null
)

@Entity(tableName = "widget_configs")
data class WidgetConfigEntity(
    @PrimaryKey val widgetId: Int,
    val boxId: String,
    val boxName: String,
    val sensorIdsString: String, // comma-separated sensor IDs, e.g. "id1,id2"
    val refreshIntervalMinutes: Int = 30,
    val visualizationType: String = "LIST", // "LIST", "GRID", "BANNER"
    val themeColorIndex: Int = 0, // color selection index
    val lastFetchedTime: Long = 0,
    val textScale: Float = 1.0f,
    val metricDisplayMode: String = "LABEL_VALUE_UNIT",
    val showRefreshButton: Boolean = true,
    val showConfigButton: Boolean = true,
    val showBoxName: Boolean = true,
    val showUpdateTime: Boolean = true,
    val useConditionalFormatting: Boolean = true,
    // How AQI (Instant) is displayed in the widget: "NUMBER_AND_LABEL", "NUMBER_ONLY", "LABEL_ONLY"
    val aqiDisplayMode: String = "NUMBER_AND_LABEL"
)

@Entity(tableName = "sensor_caches")
data class SensorCacheEntity(
    @PrimaryKey val sensorId: String,
    val boxId: String,
    val sensorTitle: String,
    val sensorUnit: String?,
    val sensorType: String?,
    val value: String?,
    val updatedAt: String?,
    val localFetchedAt: Long = 0
)
