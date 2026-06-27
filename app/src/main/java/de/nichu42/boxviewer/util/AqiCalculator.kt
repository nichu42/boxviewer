package de.nichu42.boxviewer.util

import de.nichu42.boxviewer.data.db.SensorCacheEntity
import kotlin.math.roundToInt

enum class AqiSystem(val label: String, val scaleLabel: String) {
    US_EPA("US EPA AQI", "AQI"),
    UK_DAQI("UK DAQI", "DAQI"),
    EU_EAQI("European EAQI", "EAQI"),
    CANADA_AQHI("Canada AQHI", "AQHI"),
    INDIA_AQI("India AQI", "AQI"),
    CHINA_AQI("China AQI", "AQI")
}

data class AqiResult(
    val value: Double?,         // numeric index score (null for EU qualitative)
    val label: String,          // e.g. "Good", "Moderate"
    val colorHex: String,       // e.g. "#00E400"
    val isAvailable: Boolean = true
)

object AqiCalculator {

    fun calculateInstantCast(value: Double?, pmType: String, system: AqiSystem): AqiResult {
        if (value == null || value < 0.0) {
            return AqiResult(null, "N/A", "#64748B", isAvailable = false)
        }
        val cleanPm = pmType.lowercase().trim()
        val isPm25 = cleanPm.contains("2.5") || cleanPm.contains("25")
        val isPm10 = !isPm25 && (cleanPm.contains("10") || cleanPm.contains("pm10"))
        
        if (!isPm25 && !isPm10) {
            return AqiResult(null, "N/A", "#64748B", isAvailable = false)
        }

        return when (system) {
            AqiSystem.US_EPA -> calculateUsEpa(value, isPm25)
            AqiSystem.UK_DAQI -> calculateUkDaqi(value, isPm25)
            AqiSystem.EU_EAQI -> calculateEuEaqi(value, isPm25)
            AqiSystem.CANADA_AQHI -> calculateCanadaAqhi(value, isPm25)
            AqiSystem.INDIA_AQI -> calculateIndiaAqi(value, isPm25)
            AqiSystem.CHINA_AQI -> calculateChinaAqi(value, isPm25)
        }
    }

    fun calculateNowCast(values: List<Double>, pmType: String, system: AqiSystem): AqiResult {
        if (values.isEmpty()) {
            return AqiResult(null, "N/A", "#64748B", isAvailable = false)
        }
        
        // Find min/max in last 12 values (which cover up to 12 hours)
        val limitValues = values.take(12)
        val min = limitValues.minOrNull() ?: 0.0
        val max = limitValues.maxOrNull() ?: 0.0
        
        val range = max - min
        val w = if (max > 0.0) {
            val calcW = 1.0 - (range / max)
            if (calcW < 0.5) 0.5 else calcW
        } else {
            0.5
        }

        var sumValues = 0.0
        var sumWeights = 0.0
        var currentWeight = 1.0

        for (v in limitValues) {
            sumValues += v * currentWeight
            sumWeights += currentWeight
            currentWeight *= w
        }

        val nowCastVal = if (sumWeights > 0.0) sumValues / sumWeights else limitValues.first()
        return calculateInstantCast(nowCastVal, pmType, system)
    }

    fun synthesizeVirtualSensors(
        list: List<SensorCacheEntity>,
        system: AqiSystem,
        boxId: String
    ): List<SensorCacheEntity> {
        val pm25 = list.firstOrNull { 
            val title = it.sensorTitle.lowercase()
            title.contains("pm2.5") || title.contains("pm25") || (it.sensorUnit?.contains("g/m") == true && !title.contains("pm10"))
        }
        val pm10 = list.firstOrNull { 
            val title = it.sensorTitle.lowercase()
            title.contains("pm10") || (it.sensorUnit?.contains("g/m") == true && title.contains("pm10"))
        }

        if (pm25 == null && pm10 == null) return list

        val pm25Val = pm25?.value?.toDoubleOrNull()
        val pm10Val = pm10?.value?.toDoubleOrNull()

        val aqi25 = if (pm25Val != null) calculateInstantCast(pm25Val, "pm2.5", system) else null
        val aqi10 = if (pm10Val != null) calculateInstantCast(pm10Val, "pm10", system) else null

        val result = when {
            aqi25 != null && aqi25.isAvailable && aqi10 != null && aqi10.isAvailable -> {
                val val25 = aqi25.value ?: 0.0
                val val10 = aqi10.value ?: 0.0
                if (system == AqiSystem.EU_EAQI) {
                    val sev25 = getSeverityLevel(aqi25.label)
                    val sev10 = getSeverityLevel(aqi10.label)
                    if (sev25 >= sev10) aqi25 else aqi10
                } else {
                    if (val25 >= val10) aqi25 else aqi10
                }
            }
            aqi25 != null && aqi25.isAvailable -> aqi25
            aqi10 != null && aqi10.isAvailable -> aqi10
            else -> null
        }

        if (result == null || !result.isAvailable) return list

        val valueStr = if (result.value != null) {
            String.format(java.util.Locale.US, "%.0f", result.value)
        } else {
            result.label
        }

        val virtualSensor = SensorCacheEntity(
            sensorId = "virtual_aqi",
            boxId = boxId,
            sensorTitle = "AQI (Instant)",
            sensorUnit = result.label, // e.g. "Good", "Moderate" — mapped to aqiLabel in widget wrapper
            sensorType = "AQI \u2013 Locally computed",
            value = valueStr,          // numeric string, e.g. "42"
            updatedAt = pm25?.updatedAt ?: pm10?.updatedAt,
            localFetchedAt = pm25?.localFetchedAt ?: pm10?.localFetchedAt ?: 0L
        )

        return list + virtualSensor
    }

    fun getSeverityLevel(label: String): Int {
        return when (label) {
            "Very Good" -> 1
            "Good", "Satisfactory", "Low Risk (1)", "Low Risk (2)", "Low Risk (3)" -> 2
            "Fair", "Moderate", "Moderate Risk (4)", "Moderate Risk (5)", "Moderate Risk (6)", "Moderately Polluted" -> 3
            "Poor", "Unhealthy for Sensitive Groups", "High Risk (7)", "High Risk (8)", "High Risk (9)", "High Risk (10)" -> 4
            "Very Poor", "Unhealthy", "Very Unhealthy", "Heavily Polluted", "Very High Risk (10+)" -> 5
            "Extremely Poor", "Hazardous", "Severe", "Severely Polluted" -> 6
            else -> 0
        }
    }

    private fun interpolate(c: Double, cLow: Double, cHigh: Double, iLow: Double, iHigh: Double): Double {
        return ((iHigh - iLow) / (cHigh - cLow)) * (c - cLow) + iLow
    }

    // --- US EPA ---
    private fun calculateUsEpa(rawVal: Double, isPm25: Boolean): AqiResult {
        if (isPm25) {
            val c = (rawVal * 10.0).roundToInt() / 10.0
            return when {
                c <= 9.0 -> AqiResult(interpolate(c, 0.0, 9.0, 0.0, 50.0), "Good", "#00E400")
                c <= 35.4 -> AqiResult(interpolate(c, 9.1, 35.4, 51.0, 100.0), "Moderate", "#FFFF00")
                c <= 55.4 -> AqiResult(interpolate(c, 35.5, 55.4, 101.0, 150.0), "Unhealthy for Sensitive Groups", "#FF7E00")
                c <= 125.4 -> AqiResult(interpolate(c, 55.5, 125.4, 151.0, 200.0), "Unhealthy", "#FF0000")
                c <= 225.4 -> AqiResult(interpolate(c, 125.5, 225.4, 201.0, 300.0), "Very Unhealthy", "#8F3F97")
                else -> AqiResult(interpolate(c, 225.5, 325.4, 301.0, 500.0), "Hazardous", "#7E0023")
            }
        } else {
            val c = rawVal.toInt().toDouble()
            return when {
                c <= 54.0 -> AqiResult(interpolate(c, 0.0, 54.0, 0.0, 50.0), "Good", "#00E400")
                c <= 154.0 -> AqiResult(interpolate(c, 55.0, 154.0, 51.0, 100.0), "Moderate", "#FFFF00")
                c <= 254.0 -> AqiResult(interpolate(c, 155.0, 254.0, 101.0, 150.0), "Unhealthy for Sensitive Groups", "#FF7E00")
                c <= 354.0 -> AqiResult(interpolate(c, 255.0, 354.0, 151.0, 200.0), "Unhealthy", "#FF0000")
                c <= 424.0 -> AqiResult(interpolate(c, 355.0, 424.0, 201.0, 300.0), "Very Unhealthy", "#8F3F97")
                else -> AqiResult(interpolate(c, 425.0, 504.0, 301.0, 500.0), "Hazardous", "#7E0023")
            }
        }
    }

    // --- UK DAQI ---
    private fun calculateUkDaqi(rawVal: Double, isPm25: Boolean): AqiResult {
        val c = rawVal.roundToInt()
        if (isPm25) {
            return when {
                c <= 11 -> AqiResult(1.0, "Low (Level 1)", "#008000")
                c <= 23 -> AqiResult(2.0, "Low (Level 2)", "#008000")
                c <= 35 -> AqiResult(3.0, "Low (Level 3)", "#008000")
                c <= 41 -> AqiResult(4.0, "Moderate (Level 4)", "#FFFF00")
                c <= 47 -> AqiResult(5.0, "Moderate (Level 5)", "#FFFF00")
                c <= 53 -> AqiResult(6.0, "Moderate (Level 6)", "#FFFF00")
                c <= 58 -> AqiResult(7.0, "High (Level 7)", "#FF0000")
                c <= 64 -> AqiResult(8.0, "High (Level 8)", "#FF0000")
                c <= 70 -> AqiResult(9.0, "High (Level 9)", "#FF0000")
                else -> AqiResult(10.0, "Very High (Level 10)", "#800080")
            }
        } else {
            return when {
                c <= 16 -> AqiResult(1.0, "Low (Level 1)", "#008000")
                c <= 33 -> AqiResult(2.0, "Low (Level 2)", "#008000")
                c <= 50 -> AqiResult(3.0, "Low (Level 3)", "#008000")
                c <= 58 -> AqiResult(4.0, "Moderate (Level 4)", "#FFFF00")
                c <= 66 -> AqiResult(5.0, "Moderate (Level 5)", "#FFFF00")
                c <= 75 -> AqiResult(6.0, "Moderate (Level 6)", "#FFFF00")
                c <= 83 -> AqiResult(7.0, "High (Level 7)", "#FF0000")
                c <= 91 -> AqiResult(8.0, "High (Level 8)", "#FF0000")
                c <= 100 -> AqiResult(9.0, "High (Level 9)", "#FF0000")
                else -> AqiResult(10.0, "Very High (Level 10)", "#800080")
            }
        }
    }

    // --- EU EAQI ---
    private fun calculateEuEaqi(rawVal: Double, isPm25: Boolean): AqiResult {
        val c = rawVal.roundToInt()
        if (isPm25) {
            return when {
                c <= 5 -> AqiResult(null, "Very Good", "#5AAA5F")
                c <= 15 -> AqiResult(null, "Good", "#A7D25C")
                c <= 30 -> AqiResult(null, "Moderate", "#ECD347")
                c <= 50 -> AqiResult(null, "Poor", "#EF9A3C")
                c <= 140 -> AqiResult(null, "Very Poor", "#E8665E")
                else -> AqiResult(null, "Extremely Poor", "#B765A2")
            }
        } else {
            return when {
                c <= 9 -> AqiResult(null, "Very Good", "#5AAA5F")
                c <= 27 -> AqiResult(null, "Good", "#A7D25C")
                c <= 54 -> AqiResult(null, "Moderate", "#ECD347")
                c <= 90 -> AqiResult(null, "Poor", "#EF9A3C")
                c <= 140 -> AqiResult(null, "Very Poor", "#E8665E")
                else -> AqiResult(null, "Extremely Poor", "#B765A2")
            }
        }
    }

    // --- Canada AQHI ---
    private fun calculateCanadaAqhi(rawVal: Double, isPm25: Boolean): AqiResult {
        if (!isPm25) {
            return AqiResult(null, "N/A", "#64748B", isAvailable = false)
        }
        val c = rawVal.roundToInt()
        return when {
            c <= 10 -> AqiResult(1.0, "Low Risk (1)", "#00E5FF")
            c <= 20 -> AqiResult(2.0, "Low Risk (2)", "#00E5FF")
            c <= 30 -> AqiResult(3.0, "Low Risk (3)", "#00E5FF")
            c <= 40 -> AqiResult(4.0, "Moderate Risk (4)", "#FBC02D")
            c <= 50 -> AqiResult(5.0, "Moderate Risk (5)", "#FBC02D")
            c <= 60 -> AqiResult(6.0, "Moderate Risk (6)", "#FBC02D")
            c <= 70 -> AqiResult(7.0, "High Risk (7)", "#E53935")
            c <= 80 -> AqiResult(8.0, "High Risk (8)", "#E53935")
            c <= 90 -> AqiResult(9.0, "High Risk (9)", "#E53935")
            c <= 100 -> AqiResult(10.0, "High Risk (10)", "#E53935")
            else -> AqiResult(11.0, "Very High Risk (10+)", "#8E24AA")
        }
    }

    // --- India National AQI ---
    private fun calculateIndiaAqi(rawVal: Double, isPm25: Boolean): AqiResult {
        val c = rawVal.roundToInt().toDouble()
        if (isPm25) {
            return when {
                c <= 30.0 -> AqiResult(interpolate(c, 0.0, 30.0, 0.0, 50.0), "Good", "#4CAF50")
                c <= 60.0 -> AqiResult(interpolate(c, 31.0, 60.0, 51.0, 100.0), "Satisfactory", "#8BC34A")
                c <= 90.0 -> AqiResult(interpolate(c, 61.0, 90.0, 101.0, 200.0), "Moderately Polluted", "#FFEB3B")
                c <= 120.0 -> AqiResult(interpolate(c, 91.0, 120.0, 201.0, 300.0), "Poor", "#FF9800")
                c <= 250.0 -> AqiResult(interpolate(c, 121.0, 250.0, 301.0, 400.0), "Very Poor", "#F44336")
                else -> AqiResult(interpolate(c, 251.0, 350.0, 401.0, 500.0), "Severe", "#B71C1C")
            }
        } else {
            return when {
                c <= 50.0 -> AqiResult(interpolate(c, 0.0, 50.0, 0.0, 50.0), "Good", "#4CAF50")
                c <= 100.0 -> AqiResult(interpolate(c, 51.0, 100.0, 51.0, 100.0), "Satisfactory", "#8BC34A")
                c <= 250.0 -> AqiResult(interpolate(c, 101.0, 250.0, 101.0, 200.0), "Moderately Polluted", "#FFEB3B")
                c <= 350.0 -> AqiResult(interpolate(c, 251.0, 350.0, 201.0, 300.0), "Poor", "#FF9800")
                c <= 430.0 -> AqiResult(interpolate(c, 351.0, 430.0, 301.0, 400.0), "Very Poor", "#F44336")
                else -> AqiResult(interpolate(c, 431.0, 510.0, 401.0, 500.0), "Severe", "#B71C1C")
            }
        }
    }

    // --- China AQHI ---
    private fun calculateChinaAqi(rawVal: Double, isPm25: Boolean): AqiResult {
        if (isPm25) {
            val c = (rawVal * 10.0).roundToInt() / 10.0
            return when {
                c <= 35.0 -> AqiResult(interpolate(c, 0.0, 35.0, 0.0, 50.0), "Excellent", "#00E400")
                c <= 75.0 -> AqiResult(interpolate(c, 35.1, 75.0, 51.0, 100.0), "Good", "#FFFF00")
                c <= 115.0 -> AqiResult(interpolate(c, 75.1, 115.0, 101.0, 150.0), "Lightly Polluted", "#FF7E00")
                c <= 150.0 -> AqiResult(interpolate(c, 115.1, 150.0, 151.0, 200.0), "Moderately Polluted", "#FF0000")
                c <= 250.0 -> AqiResult(interpolate(c, 150.1, 250.0, 201.0, 300.0), "Heavily Polluted", "#8F3F97")
                else -> AqiResult(interpolate(c, 250.1, 500.0, 301.0, 500.0), "Severely Polluted", "#7E0023")
            }
        } else {
            val c = rawVal.toInt().toDouble()
            return when {
                c <= 50.0 -> AqiResult(interpolate(c, 0.0, 50.0, 0.0, 50.0), "Excellent", "#00E400")
                c <= 150.0 -> AqiResult(interpolate(c, 50.1, 150.0, 51.0, 100.0), "Good", "#FFFF00")
                c <= 250.0 -> AqiResult(interpolate(c, 150.1, 250.0, 101.0, 150.0), "Lightly Polluted", "#FF7E00")
                c <= 350.0 -> AqiResult(interpolate(c, 250.1, 350.0, 151.0, 200.0), "Moderately Polluted", "#FF0000")
                c <= 420.0 -> AqiResult(interpolate(c, 350.1, 420.0, 201.0, 300.0), "Heavily Polluted", "#8F3F97")
                else -> AqiResult(interpolate(c, 420.1, 600.0, 301.0, 500.0), "Severely Polluted", "#7E0023")
            }
        }
    }
}
