package de.nichu42.boxviewer.util

/**
 * Converts a raw sensor measurement through the temperature/pressure/wind unit chain
 * and returns the final display value and unified unit. Keeps intermediate units so
 * callers can convert related measurements (e.g. sparkline histories) consistently.
 */
object SensorDisplayConverter {

    data class ConversionResult(
        val value: String?,
        val unit: String?,
        val tempUnit: String?,
        val pressUnit: String?
    )

    /**
     * Runs the numeric conversion chain and formats the final display value using
     * the current app locale. Intermediate values stay numeric so locale formatting
     * never breaks parsing between conversion steps.
     */
    fun convert(
        rawValue: String?,
        sourceUnit: String?,
        temperatureUnit: String,
        pressureUnit: String,
        windUnit: String,
        formatPressure: Boolean
    ): ConversionResult {
        return convert(rawValue, sourceUnit, temperatureUnit, pressureUnit, windUnit, formatPressure, java.util.Locale.getDefault())
    }

    fun convert(
        rawValue: String?,
        sourceUnit: String?,
        temperatureUnit: String,
        pressureUnit: String,
        windUnit: String,
        formatPressure: Boolean,
        locale: java.util.Locale
    ): ConversionResult {
        // Temperature step
        val tempDouble = TemperatureConverter.convertToDouble(rawValue, sourceUnit, temperatureUnit)
        val tempUnit = if (tempDouble != null) {
            TemperatureConverter.convertUnit(sourceUnit, temperatureUnit)
        } else {
            sourceUnit
        }

        // Pressure step (passes through non-pressure values unchanged)
        val pressDouble = PressureConverter.convertToDouble(
            tempDouble?.toString() ?: rawValue,
            tempUnit,
            pressureUnit
        )
        val pressUnit = if (pressDouble != null) {
            PressureConverter.convertUnit(tempUnit, pressureUnit)
        } else {
            tempUnit
        }

        // Wind step (passes through non-wind values unchanged)
        val windDouble = WindConverter.convertToDouble(
            pressDouble?.toString() ?: tempDouble?.toString() ?: rawValue,
            pressUnit,
            windUnit
        )
        val windUnitResult = if (windDouble != null) {
            WindConverter.convertUnit(pressUnit, windUnit)
        } else {
            pressUnit
        }

        val displayUnit = UnitUnifier.unifyUnit(windUnitResult)

        // Format the final numeric value with the converter that actually handled it.
        val displayValue = when {
            windDouble != null -> WindConverter.formatValue(windDouble, locale)
            pressDouble != null -> PressureConverter.formatValue(pressDouble, formatPressure, locale)
            tempDouble != null -> TemperatureConverter.formatValue(tempDouble, locale)
            else -> rawValue
        }

        return ConversionResult(
            value = displayValue,
            unit = displayUnit,
            tempUnit = tempUnit,
            pressUnit = pressUnit
        )
    }
}
