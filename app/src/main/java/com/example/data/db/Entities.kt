package com.example.data.db

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
    val textScale: Float = 1.0f
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
