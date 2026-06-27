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

    fun convert(
        rawValue: String?,
        sourceUnit: String?,
        temperatureUnit: String,
        pressureUnit: String,
        windUnit: String,
        formatPressure: Boolean
    ): ConversionResult {
        val tempVal = TemperatureConverter.convertValue(rawValue, sourceUnit, temperatureUnit)
        val tempUnit = TemperatureConverter.convertUnit(sourceUnit, temperatureUnit)
        val pressVal = PressureConverter.convertValue(tempVal, tempUnit, pressureUnit, formatPressure)
        val pressUnit = PressureConverter.convertUnit(tempUnit, pressureUnit)
        val windVal = WindConverter.convertValue(pressVal, pressUnit, windUnit)
        val windUnitResult = WindConverter.convertUnit(pressUnit, windUnit)
        val displayUnit = UnitUnifier.unifyUnit(windUnitResult)
        return ConversionResult(value = windVal, unit = displayUnit, tempUnit = tempUnit, pressUnit = pressUnit)
    }
}
