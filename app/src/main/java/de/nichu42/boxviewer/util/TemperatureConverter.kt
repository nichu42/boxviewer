package de.nichu42.boxviewer.util

import java.util.Locale

object TemperatureConverter {
    fun convertValue(valueStr: String?, fromUnit: String?, targetUnit: String): String {
        if (valueStr == null) return "--"
        
        val fromUnitClean = fromUnit?.trim() ?: "°C"
        val isFromF = fromUnitClean.equals("°F", ignoreCase = true) || fromUnitClean.equals("F", ignoreCase = true)
        val isFromK = fromUnitClean.equals("K", ignoreCase = true) || fromUnitClean.equals("Kelvin", ignoreCase = true)
        val isFromC = fromUnitClean.equals("°C", ignoreCase = true) || fromUnitClean.equals("C", ignoreCase = true)
        
        // If the source unit is explicitly a non-temperature unit, bypass conversion and return valueStr unmodified
        val hasOriginalUnit = fromUnit != null && fromUnit.trim().isNotEmpty()
        val isTempUnit = isFromF || isFromK || isFromC
        if (hasOriginalUnit && !isTempUnit) {
            return valueStr
        }

        return try {
            var celsius = valueStr.toDouble()
            if (isFromF) {
                celsius = (celsius - 32.0) / 1.8
            } else if (isFromK) {
                celsius -= 273.15
            }

            when (targetUnit) {
                "°F" -> {
                    val fahrenheit = celsius * 1.8 + 32.0
                    String.format(Locale.US, "%.1f", fahrenheit)
                }
                "K" -> {
                    val kelvin = celsius + 273.15
                    String.format(Locale.US, "%.1f", kelvin)
                }
                else -> {
                    String.format(Locale.US, "%.1f", celsius)
                }
            }
        } catch (e: Exception) {
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
