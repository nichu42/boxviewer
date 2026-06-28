package de.nichu42.boxviewer.util

import androidx.core.graphics.toColorInt

/**
 * Shared, UI-framework-agnostic resolver for sensor value colors.
 * Returns an ARGB integer that can be wrapped by Compose `Color(...)` or used directly in RemoteViews.
 *
 * @param title        Sensor title, used to detect PM/temperature/pressure/humidity.
 * @param valueString  The displayed value (numeric or, for EU EAQI, a label like "Good").
 * @param unit         Optional source unit; temperature is converted to °C and pressure to hPa
 *                     before thresholds are applied. Pass null if the value is already canonical.
 * @param aqiSystem    Currently selected AQI standard.
 * @param defaultColor ARGB integer to return when no specific threshold applies.
 */
object SensorValueColorResolver {
    fun resolveColor(
        title: String,
        valueString: String?,
        unit: String?,
        aqiSystem: AqiSystem,
        defaultColor: Int
    ): Int {
        val lower = title.lowercase()

        val value = valueString?.let { raw ->
            when {
                lower.contains("temp") -> TemperatureConverter.convertValue(raw, unit, "°C").toDoubleOrNull()
                lower.contains("druck") || lower.contains("press") ->
                    PressureConverter.convertValue(raw, unit, "hPa", formatPressure = false).toDoubleOrNull()
                else -> raw.toDoubleOrNull()
            }
        }

        val isPm25 = lower.contains("pm2.5") || lower.contains("pm25") ||
                (lower.contains("pm") && !lower.contains("10"))
        val isPm10 = lower.contains("pm10")
        val isVirtualAqi = lower.contains("air quality index") || lower.contains("aqi")

        if (isPm25 || isPm10 || isVirtualAqi) {
            // EU EAQI uses qualitative labels; color by label text when no numeric value is available.
            if (value == null && valueString != null) {
                val hex = when (aqiSystem) {
                    AqiSystem.EU_EAQI -> when (valueString) {
                        "Very Good" -> "#5AAA5F"
                        "Good" -> "#A7D25C"
                        "Moderate" -> "#ECD347"
                        "Poor" -> "#EF9A3C"
                        "Very Poor" -> "#E8665E"
                        "Extremely Poor" -> "#B765A2"
                        else -> null
                    }
                    else -> null
                }
                if (hex != null) return hex.toColorInt()
            }

            if (isVirtualAqi && value != null) {
                val hex = when (aqiSystem) {
                    AqiSystem.US_EPA, AqiSystem.CHINA_AQI -> when {
                        value <= 50.0 -> "#00E400"
                        value <= 100.0 -> "#FFFF00"
                        value <= 150.0 -> "#FF7E00"
                        value <= 200.0 -> "#FF0000"
                        value <= 300.0 -> "#8F3F97"
                        else -> "#7E0023"
                    }
                    AqiSystem.UK_DAQI -> when {
                        value <= 3.0 -> "#008000"
                        value <= 6.0 -> "#FFFF00"
                        value <= 9.0 -> "#FF0000"
                        else -> "#800080"
                    }
                    AqiSystem.CANADA_AQHI -> when {
                        value <= 3.0 -> "#00E5FF"
                        value <= 6.0 -> "#FBC02D"
                        value <= 10.0 -> "#E53935"
                        else -> "#8E24AA"
                    }
                    AqiSystem.INDIA_AQI -> when {
                        value <= 50.0 -> "#4CAF50"
                        value <= 100.0 -> "#8BC34A"
                        value <= 200.0 -> "#FFEB3B"
                        value <= 300.0 -> "#FF9800"
                        value <= 400.0 -> "#F44336"
                        else -> "#B71C1C"
                    }
                    else -> "#64748B"
                }
                return hex.toColorInt()
            }

            if (value != null) {
                val pmType = if (isPm10) "pm10" else "pm2.5"
                val res = AqiCalculator.calculateInstantCast(value, pmType, aqiSystem)
                return res.colorHex.toColorInt()
            }

            return defaultColor
        }

        val rawVal = value ?: return defaultColor
        return when {
            lower.contains("temp") -> when {
                rawVal <= 0.0 -> 0xFF1E88E5.toInt()
                rawVal <= 15.0 -> 0xFF64B5F6.toInt()
                rawVal <= 25.0 -> 0xFF4CAF50.toInt()
                rawVal <= 32.0 -> 0xFFFFA726.toInt()
                else -> 0xFFE53935.toInt()
            }
            lower.contains("feucht") || lower.contains("humid") -> when {
                rawVal < 30.0 -> 0xFFFFB74D.toInt()
                rawVal <= 60.0 -> 0xFF4CAF50.toInt()
                else -> 0xFF0288D1.toInt()
            }
            lower.contains("druck") || lower.contains("press") -> when {
                rawVal < 1000.0 -> 0xFF90A4AE.toInt()
                rawVal <= 1020.0 -> 0xFF4CAF50.toInt()
                else -> 0xFFFBC02D.toInt()
            }
            else -> defaultColor
        }
    }
}