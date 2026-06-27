package de.nichu42.boxviewer.util

/**
 * Canonical display order for environmental sensors.
 * Temperature → Humidity → PM10 → PM2.5 → AQI → Pressure → Wind → other.
 */
object SensorSortKey {
    fun of(title: String?): Int {
        val t = title?.lowercase() ?: return Int.MAX_VALUE
        return when {
            t.contains("temp") -> 0
            t.contains("humid") || t.contains("feucht") -> 1
            t.contains("pm10") -> 2
            t.contains("pm2") || t.contains("pm25") -> 3
            t.contains("aqi") -> 4
            t.contains("press") || t.contains("druck") -> 5
            t.contains("wind") || t.contains("speed") -> 6
            else -> 7
        }
    }
}
