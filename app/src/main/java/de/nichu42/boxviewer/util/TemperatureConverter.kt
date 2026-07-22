package de.nichu42.boxviewer.util

import java.util.Locale

object TemperatureConverter {

    /**
     * Converts [valueStr] from [fromUnit] to [targetUnit] and returns the numeric
     * result, or null if the source is not a temperature unit or cannot be parsed.
     */
    fun convertToDouble(valueStr: String?, fromUnit: String?, targetUnit: String): Double? {
        if (valueStr == null) return null

        val fromUnitClean = fromUnit?.trim() ?: "°C"
        val isFromF = fromUnitClean.equals("°F", ignoreCase = true) || fromUnitClean.equals("F", ignoreCase = true)
        val isFromK = fromUnitClean.equals("K", ignoreCase = true) || fromUnitClean.equals("Kelvin", ignoreCase = true)
        val isFromC = fromUnitClean.equals("°C", ignoreCase = true) || fromUnitClean.equals("C", ignoreCase = true)

        val hasOriginalUnit = fromUnit != null && fromUnit.trim().isNotEmpty()
        val isTempUnit = isFromF || isFromK || isFromC
        if (hasOriginalUnit && !isTempUnit) {
            return null
        }

        return try {
            val cleanVal = valueStr.replace(',', '.').trim()
            var celsius = cleanVal.toDouble()
            if (isFromF) {
                celsius = (celsius - 32.0) / 1.8
            } else if (isFromK) {
                celsius -= 273.15
            }

            when (targetUnit) {
                "°F" -> celsius * 1.8 + 32.0
                "K" -> celsius + 273.15
                else -> celsius
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Formats a temperature numeric value for the given locale.
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
            // Non-temperature unit or parse error: return the original value unchanged.
            valueStr
        }
    }

    fun convertUnit(originalUnit: String?, targetUnit: String): String {
        val clean = originalUnit?.trim() ?: ""
        val isTemp = clean.equals("°C", ignoreCase = true) ||
                clean.equals("°F", ignoreCase = true) ||
                clean.equals("K", ignoreCase = true) ||
                clean.equals("Kelvin", ignoreCase = true) ||
                clean.equals("C", ignoreCase = true) ||
                clean.equals("F", ignoreCase = true)

        if (isTemp) {
            return targetUnit
        }
        return originalUnit ?: ""
    }
}
