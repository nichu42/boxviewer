package de.nichu42.boxviewer.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SenseBox(
    @param:Json(name = "_id") val id: String,
    val name: String,
    val description: String?,
    val exposure: String?,
    val model: String?,
    @param:Json(name = "grouptag") val grouptagRaw: Any?,
    val currentLocation: CurrentLocation?,
    val sensors: List<Sensor>?
) {
    val grouptag: String?
        get() = when (val tag = grouptagRaw) {
            is String -> tag
            is List<*> -> tag.firstOrNull()?.toString()
            else -> tag?.toString()
        }
}

@JsonClass(generateAdapter = true)
data class CurrentLocation(
    val type: String?,
    val coordinates: List<Double>? // [longitude, latitude]
) {
    val longitude: Double get() = coordinates?.getOrNull(0) ?: 0.0
    val latitude: Double get() = coordinates?.getOrNull(1) ?: 0.0
}

@JsonClass(generateAdapter = true)
data class Sensor(
    @param:Json(name = "_id") val id: String,
    val title: String,
    val unit: String?,
    val sensorType: String?,
    val lastMeasurement: Measurement?
)

@JsonClass(generateAdapter = true)
data class Measurement(
    val value: String?,
    val createdAt: String?
)
