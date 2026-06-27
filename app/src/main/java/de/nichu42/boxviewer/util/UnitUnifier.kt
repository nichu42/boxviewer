package de.nichu42.boxviewer.util

object UnitUnifier {
    fun unifyUnit(originalUnit: String?): String {
        if (originalUnit == null) return ""
        val trimmed = originalUnit.trim()
        
        // 1. Humidity Unification
        val isHumidity = trimmed.contains("%") || 
                         trimmed.equals("r.H.", ignoreCase = true) || 
                         trimmed.equals("RH", ignoreCase = true) || 
                         trimmed.equals("rH", ignoreCase = true) || 
                         trimmed.equals("percent", ignoreCase = true)
        if (isHumidity) {
            return "%"
        }

        // 2. Particulate Matter (PM) Unification
        val isPMUnit = trimmed.equals("µg/m³", ignoreCase = true) || 
                       trimmed.equals("ug/m3", ignoreCase = true) || 
                       trimmed.equals("µg/m3", ignoreCase = true) || 
                       trimmed.equals("ug/m³", ignoreCase = true)
        if (isPMUnit) {
            return "µg/m³"
        }

        // 3. Solar Irradiance Unification
        val isIrradiance = trimmed.equals("W/m2", ignoreCase = true) ||
                           trimmed.equals("W/m²", ignoreCase = true)
        if (isIrradiance) {
            return "W/m²"
        }

        // 4. Illuminance (Lux) Unification
        val isLux = trimmed.equals("lux", ignoreCase = true) ||
                    trimmed.equals("Lux", ignoreCase = true) ||
                    trimmed.equals("lx", ignoreCase = true)
        if (isLux) {
            return "lx"
        }

        // 5. Precipitation Unification
        val isPrecipitation = trimmed.equals("l/m2", ignoreCase = true) ||
                              trimmed.equals("l/m²", ignoreCase = true)
        if (isPrecipitation) {
            return "l/m²"
        }
        
        return trimmed
    }
}
