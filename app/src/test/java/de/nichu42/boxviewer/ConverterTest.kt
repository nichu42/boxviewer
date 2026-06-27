package de.nichu42.boxviewer

import de.nichu42.boxviewer.util.PressureConverter
import de.nichu42.boxviewer.util.TemperatureConverter
import de.nichu42.boxviewer.util.UnitUnifier
import de.nichu42.boxviewer.util.WindConverter
import de.nichu42.boxviewer.util.AqiSystem
import de.nichu42.boxviewer.util.AqiCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class ConverterTest {

    @Test
    fun testTemperatureConverter() {
        // Celsius to Fahrenheit & Kelvin
        assertEquals("32.0", TemperatureConverter.convertValue("0.0", "°C", "°F"))
        assertEquals("68.0", TemperatureConverter.convertValue("20.0", "°C", "°F"))
        assertEquals("-4.0", TemperatureConverter.convertValue("-20.0", "°C", "°F"))
        assertEquals("273.2", TemperatureConverter.convertValue("0.0", "°C", "K"))
        assertEquals("293.2", TemperatureConverter.convertValue("20.0", "°C", "K"))

        // Kelvin / Fahrenheit back-normalization
        assertEquals("20.0", TemperatureConverter.convertValue("293.15", "K", "°C"))
        assertEquals("20.0", TemperatureConverter.convertValue("68.0", "°F", "°C"))
        
        // Units
        assertEquals("°F", TemperatureConverter.convertUnit("°C", "°F"))
        assertEquals("°C", TemperatureConverter.convertUnit("°C", "°C"))
        assertEquals("K", TemperatureConverter.convertUnit("°C", "K"))
        assertEquals("m/s", TemperatureConverter.convertUnit("m/s", "°C"))
    }

    @Test
    fun testPressureConverter() {
        // Conversions to standard Hectopascals
        assertEquals("1,004.14", PressureConverter.convertValue("100414", "Pa", "hPa", formatPressure = true))
        assertEquals("1,004.14", PressureConverter.convertValue("1004.14", "hPa", "hPa", formatPressure = true))
        assertEquals("1,004.14", PressureConverter.convertValue("1004.14", "mbar", "hPa", formatPressure = true))
        
        // Conversions to other units
        assertEquals("100,414", PressureConverter.convertValue("1004.14", "hPa", "Pa", formatPressure = true))
        assertEquals("1,004.14", PressureConverter.convertValue("1004.14", "hPa", "mbar", formatPressure = true))
        assertEquals("29.65", PressureConverter.convertValue("1004.14", "hPa", "inHg", formatPressure = true))
        assertEquals("753.17", PressureConverter.convertValue("1004.14", "hPa", "mmHg", formatPressure = true))
        
        // Disabled formatting
        assertEquals("1004.14", PressureConverter.convertValue("100414", "Pa", "hPa", formatPressure = false))
        
        // Units
        assertEquals("hPa", PressureConverter.convertUnit("Pa", "hPa"))
        assertEquals("Pa", PressureConverter.convertUnit("hPa", "Pa"))
        assertEquals("inHg", PressureConverter.convertUnit("hPa", "inHg"))
    }

    @Test
    fun testWindConverter() {
        // Conversions to various units
        assertEquals("3.6", WindConverter.convertValue("1.0", "m/s", "km/h"))
        assertEquals("2.2", WindConverter.convertValue("1.0", "m/s", "mph"))
        assertEquals("1.9", WindConverter.convertValue("1.0", "m/s", "kn"))
        
        // Normalization from other units to m/s
        assertEquals("1.0", WindConverter.convertValue("3.6", "km/h", "m/s"))
        assertEquals("1.0", WindConverter.convertValue("2.23694", "mph", "m/s"))
        assertEquals("1.0", WindConverter.convertValue("1.94384", "kn", "m/s"))
        
        // Units
        assertEquals("km/h", WindConverter.convertUnit("m/s", "km/h"))
        assertEquals("m/s", WindConverter.convertUnit("km/h", "m/s"))
    }

    @Test
    fun testUnitUnifier() {
        // Humidity unification
        assertEquals("%", UnitUnifier.unifyUnit("%"))
        assertEquals("%", UnitUnifier.unifyUnit("% r.H."))
        assertEquals("%", UnitUnifier.unifyUnit("% RH"))
        assertEquals("%", UnitUnifier.unifyUnit("rH"))
        assertEquals("%", UnitUnifier.unifyUnit("percent"))
        
        // Particulate Matter (PM) unification
        assertEquals("µg/m³", UnitUnifier.unifyUnit("µg/m³"))
        assertEquals("µg/m³", UnitUnifier.unifyUnit("ug/m3"))
        assertEquals("µg/m³", UnitUnifier.unifyUnit("µg/m3"))
        assertEquals("µg/m³", UnitUnifier.unifyUnit("ug/m³"))

        // Solar Irradiance unification
        assertEquals("W/m²", UnitUnifier.unifyUnit("W/m²"))
        assertEquals("W/m²", UnitUnifier.unifyUnit("W/m2"))

        // Illuminance unification
        assertEquals("lx", UnitUnifier.unifyUnit("lx"))
        assertEquals("lx", UnitUnifier.unifyUnit("lux"))
        assertEquals("lx", UnitUnifier.unifyUnit("Lux"))

        // Precipitation unification
        assertEquals("l/m²", UnitUnifier.unifyUnit("l/m²"))
        assertEquals("l/m²", UnitUnifier.unifyUnit("l/m2"))
        
        // Unaffected units
        assertEquals("m/s", UnitUnifier.unifyUnit("m/s"))
        assertEquals("°C", UnitUnifier.unifyUnit("°C"))
    }

    @Test
    fun testAqiCalculator() {
        // US EPA PM2.5 Breakpoints
        val usGood = AqiCalculator.calculateInstantCast(5.0, "PM2.5", AqiSystem.US_EPA)
        assertEquals(27.8, usGood.value!!, 0.1)
        assertEquals("Good", usGood.label)
        assertEquals("#00E400", usGood.colorHex)

        val usModerate = AqiCalculator.calculateInstantCast(12.0, "PM2.5", AqiSystem.US_EPA)
        assertEquals(56.4, usModerate.value!!, 0.1)
        assertEquals("Moderate", usModerate.label)
        assertEquals("#FFFF00", usModerate.colorHex)

        // US EPA PM10 Breakpoints
        val usPm10Good = AqiCalculator.calculateInstantCast(40.0, "PM10", AqiSystem.US_EPA)
        assertEquals(37.0, usPm10Good.value!!, 0.1)
        assertEquals("Good", usPm10Good.label)

        // UK DAQI PM2.5 Breakpoints
        val ukLow = AqiCalculator.calculateInstantCast(10.0, "PM2.5", AqiSystem.UK_DAQI)
        assertEquals(1.0, ukLow.value!!, 0.0)
        assertEquals("Low (Level 1)", ukLow.label)

        // EU EAQI PM2.5 Breakpoints (Qualitative)
        val euGood = AqiCalculator.calculateInstantCast(10.0, "PM2.5", AqiSystem.EU_EAQI)
        assertEquals(null, euGood.value)
        assertEquals("Good", euGood.label)

        // Canada AQHI PM2.5 Breakpoints
        val caLow = AqiCalculator.calculateInstantCast(15.0, "PM2.5", AqiSystem.CANADA_AQHI)
        assertEquals(2.0, caLow.value!!, 0.0)
        assertEquals("Low Risk (2)", caLow.label)

        // India AQI PM2.5 Breakpoints
        val inGood = AqiCalculator.calculateInstantCast(15.0, "PM2.5", AqiSystem.INDIA_AQI)
        assertEquals(25.0, inGood.value!!, 0.1)
        assertEquals("Good", inGood.label)

        // China AQI PM2.5 Breakpoints
        val cnGood = AqiCalculator.calculateInstantCast(20.0, "PM2.5", AqiSystem.CHINA_AQI)
        assertEquals(28.6, cnGood.value!!, 0.1)
        assertEquals("Excellent", cnGood.label)

        // NowCast Calculation with stable readings
        val stableValues = List(12) { 10.0 }
        val nowCastResult = AqiCalculator.calculateNowCast(stableValues, "PM2.5", AqiSystem.US_EPA)
        assertEquals("Moderate", nowCastResult.label)
        assertEquals(52.7, nowCastResult.value!!, 0.1)
    }
}
