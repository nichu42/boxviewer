package de.nichu42.boxviewer.util

import java.util.Locale

object WindConverter {
    fun convertValue(valueStr: String?, fromUnit: String?, targetUnit: String): String {
        if (valueStr == null) return "--"
        
        val fromUnitClean = fromUnit?.trim() ?: "m/s"
        val isMs = fromUnitClean.equals("m/s", ignoreCase = true) || fromUnitClean.equals("ms", ignoreCase = true)
        val isKmh = fromUnitClean.equals("km/h", ignoreCase = true) || fromUnitClean.equals("kmh", ignoreCase = true)
        val isMph = fromUnitClean.equals("mph", ignoreCase = true)
        val isKn = fromUnitClean.equals("kn", ignoreCase = true) || fromUnitClean.equals("knots", ignoreCase = true) || fromUnitClean.equals("kts", ignoreCase = true)
        
        if (!isMs && !isKmh && !isMph && !isKn) return valueStr

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
            val targetValue = when (targetUnit) {
                "km/h" -> msValue * 3.6
                "mph" -> msValue / 0.44704
                "kn" -> msValue / 0.514444
                else -> msValue // m/s
            }

            String.format(Locale.US, "%.1f", targetValue)
        } catch (e: Exception) {
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
