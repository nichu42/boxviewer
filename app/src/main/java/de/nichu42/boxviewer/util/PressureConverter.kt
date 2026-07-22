package de.nichu42.boxviewer.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object PressureConverter {

    private fun separatorFormatter(locale: Locale) =
        DecimalFormat("#,##0.##", DecimalFormatSymbols(locale))

    private fun rawFormatter(locale: Locale) =
        DecimalFormat("0.##", DecimalFormatSymbols(locale))

    /**
     * Converts [valueStr] from [fromUnit] to [targetUnit] and returns the numeric
     * result, or null if the source is not a pressure unit or cannot be parsed.
     */
    fun convertToDouble(valueStr: String?, fromUnit: String?, targetUnit: String): Double? {
        if (valueStr == null) return null

        val fromUnitClean = fromUnit?.trim() ?: "hPa"
        val isPa = fromUnitClean.equals("Pa", ignoreCase = true)
        val isHpa = fromUnitClean.equals("hPa", ignoreCase = true)
        val isMbar = fromUnitClean.equals("mbar", ignoreCase = true) || fromUnitClean.equals("mb", ignoreCase = true)
        val isInHg = fromUnitClean.equals("inHg", ignoreCase = true)
        val isMmHg = fromUnitClean.equals("mmHg", ignoreCase = true) || fromUnitClean.equals("torr", ignoreCase = true)

        if (!isPa && !isHpa && !isMbar && !isInHg && !isMmHg) return null

        return try {
            val cleanVal = valueStr.replace(',', '.').trim()
            val value = cleanVal.toDouble()

            // First normalize to hPa
            val hpaValue = when {
                isPa -> value / 100.0
                isInHg -> value * 33.8639
                isMmHg -> value * 1.33322
                else -> value // Already hPa or mbar
            }

            // Then convert to target unit
            when (targetUnit) {
                "Pa" -> hpaValue * 100.0
                "inHg" -> hpaValue / 33.8639
                "mmHg" -> hpaValue / 1.33322
                else -> hpaValue // hPa or mbar (1 hPa = 1 mbar)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Formats a pressure numeric value for the given locale.
     */
    fun formatValue(value: Double, formatPressure: Boolean, locale: Locale = Locale.getDefault()): String {
        return if (formatPressure) {
            separatorFormatter(locale).format(value)
        } else {
            rawFormatter(locale).format(value)
        }
    }

    fun convertValue(
        valueStr: String?,
        fromUnit: String?,
        targetUnit: String,
        formatPressure: Boolean,
        locale: Locale = Locale.getDefault()
    ): String {
        if (valueStr == null) return "--"

        val value = convertToDouble(valueStr, fromUnit, targetUnit)
        return if (value != null) {
            formatValue(value, formatPressure, locale)
        } else {
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
