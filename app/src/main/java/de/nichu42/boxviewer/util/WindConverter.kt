package de.nichu42.boxviewer.util

import java.util.Locale

object WindConverter {

    /**
     * Converts [valueStr] from [fromUnit] to [targetUnit] and returns the numeric
     * result, or null if the source is not a wind unit or cannot be parsed.
     */
    fun convertToDouble(valueStr: String?, fromUnit: String?, targetUnit: String): Double? {
        if (valueStr == null) return null

        val fromUnitClean = fromUnit?.trim() ?: "m/s"
        val isMs = fromUnitClean.equals("m/s", ignoreCase = true) || fromUnitClean.equals("ms", ignoreCase = true)
        val isKmh = fromUnitClean.equals("km/h", ignoreCase = true) || fromUnitClean.equals("kmh", ignoreCase = true)
        val isMph = fromUnitClean.equals("mph", ignoreCase = true)
        val isKn = fromUnitClean.equals("kn", ignoreCase = true) || fromUnitClean.equals("knots", ignoreCase = true) || fromUnitClean.equals("kts", ignoreCase = true)

        if (!isMs && !isKmh && !isMph && !isKn) return null

        return try {
            val value = valueStr.toDouble()

            // First normalize to m/s
            val msValue = when {
                isKmh -> value / 3.6
                isMph -> value * 0.44704
                isKn -> value * 0.514444
                else -> value // Already m/s
            }

            // Then convert to target unit
            when (targetUnit) {
                "km/h" -> msValue * 3.6
                "mph" -> msValue / 0.44704
                "kn" -> msValue / 0.514444
                else -> msValue // m/s
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Formats a wind numeric value for the given locale.
     */
    fun formatValue(value: Double, locale: Locale = Locale.getDefault()): String {
        return String.format(locale, "%.1f", value)
    }

    fun convertValue(
        valueStr: String?,
        fromUnit: String?,
        targetUnit: String,
        locale: Locale = Locale.getDefault()
    ): String {
        if (valueStr == null) return "--"

        val value = convertToDouble(valueStr, fromUnit, targetUnit)
        return if (value != null) {
            formatValue(value, locale)
        } else {
            valueStr
        }
    }

    fun convertUnit(originalUnit: String?, targetUnit: String): String {
        val clean = originalUnit?.trim() ?: ""
        val isWind = clean.equals("m/s", ignoreCase = true) || 
                     clean.equals("ms", ignoreCase = true) || 
                     clean.equals("km/h", ignoreCase = true) || 
                     clean.equals("kmh", ignoreCase = true) || 
                     clean.equals("mph", ignoreCase = true) || 
                     clean.equals("kn", ignoreCase = true) || 
                     clean.equals("knots", ignoreCase = true) || 
                     clean.equals("kts", ignoreCase = true)
        
        if (isWind) {
            return targetUnit
        }
        return originalUnit ?: ""
    }
}
