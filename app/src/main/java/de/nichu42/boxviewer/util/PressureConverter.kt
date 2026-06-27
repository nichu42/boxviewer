package de.nichu42.boxviewer.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object PressureConverter {
    private val separatorFormatter = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US))
    private val rawFormatter = DecimalFormat("0.##", DecimalFormatSymbols(Locale.US))

    fun convertValue(valueStr: String?, fromUnit: String?, targetUnit: String, formatPressure: Boolean): String {
        if (valueStr == null) return "--"
        
        val fromUnitClean = fromUnit?.trim() ?: "hPa"
        val isPa = fromUnitClean.equals("Pa", ignoreCase = true)
        val isHpa = fromUnitClean.equals("hPa", ignoreCase = true)
        val isMbar = fromUnitClean.equals("mbar", ignoreCase = true) || fromUnitClean.equals("mb", ignoreCase = true)
        val isInHg = fromUnitClean.equals("inHg", ignoreCase = true)
        val isMmHg = fromUnitClean.equals("mmHg", ignoreCase = true) || fromUnitClean.equals("torr", ignoreCase = true)
        
        if (!isPa && !isHpa && !isMbar && !isInHg && !isMmHg) return valueStr

        return try {
            val value = valueStr.toDouble()
            
            // First normalize to hPa
            val hpaValue = when {
                isPa -> value / 100.0
                isInHg -> value * 33.8639
                isMmHg -> value * 1.33322
                else -> value // Already hPa or mbar
            }

            // Then convert to target unit
            val targetValue = when (targetUnit) {
                "Pa" -> hpaValue * 100.0
                "inHg" -> hpaValue / 33.8639
                "mmHg" -> hpaValue / 1.33322
                else -> hpaValue // hPa or mbar (1 hPa = 1 mbar)
            }

            if (formatPressure) {
                separatorFormatter.format(targetValue)
            } else {
                rawFormatter.format(targetValue)
            }
        } catch (e: Exception) {
            valueStr
        }
    }

    fun convertUnit(originalUnit: String?, targetUnit: String): String {
        val clean = originalUnit?.trim() ?: ""
        val isPress = clean.equals("hPa", ignoreCase = true) || 
                      clean.equals("Pa", ignoreCase = true) || 
                      clean.equals("mbar", ignoreCase = true) || 
                      clean.equals("mb", ignoreCase = true) || 
                      clean.equals("inHg", ignoreCase = true) || 
                      clean.equals("mmHg", ignoreCase = true) || 
                      clean.equals("torr", ignoreCase = true)
        
        if (isPress) {
            return targetUnit
        }
        return originalUnit ?: ""
    }
}
