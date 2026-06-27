package de.nichu42.boxviewer.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import de.nichu42.boxviewer.util.AqiSystem
import de.nichu42.boxviewer.util.SensorValueColorResolver

data class SensorComposeVisuals(val icon: ImageVector, val color: Color)

object SensorTheme {
    fun getContrastColor(color: Color): Color {
        val luminance = 0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue
        return if (luminance > 0.5f) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    }

    fun getVisuals(title: String): SensorComposeVisuals {
        val lower = title.lowercase()
        return when {
            lower.contains("temp") -> SensorComposeVisuals(Icons.Default.Thermostat, Color(0xFFF97316)) // Orange
            lower.contains("feucht") || lower.contains("humid") -> SensorComposeVisuals(Icons.Default.WaterDrop, Color(0xFF0EA5E9)) // Sky Blue
            lower.contains("druck") || lower.contains("press") -> SensorComposeVisuals(Icons.Default.Compress, Color(0xFF8B5CF6)) // Purple
            lower.contains("aqi") -> SensorComposeVisuals(Icons.Default.Masks, Color(0xFF0D9488)) // Teal – AQI virtual sensor
            lower.contains("pm10") || lower.contains("pm2") || lower.contains("air") || lower.contains("dust") || lower.contains("feinstaub") -> SensorComposeVisuals(Icons.Default.Air, Color(0xFFEC4899)) // Pink
            lower.contains("cop") || lower.contains("co2") || lower.contains("carbon") -> SensorComposeVisuals(Icons.Default.Co2, Color(0xFF34D399)) // Emerald/Mint
            lower.contains("bell") || lower.contains("light") || lower.contains("lux") || lower.contains("sonne") -> SensorComposeVisuals(Icons.Default.WbSunny, Color(0xFFF59E0B)) // Yellow/Amber
            lower.contains("uv") -> SensorComposeVisuals(Icons.Default.LightMode, Color(0xFFF59E0B)) // Yellow/Amber
            lower.contains("speed") || lower.contains("wind") -> SensorComposeVisuals(Icons.Default.Speed, Color(0xFF64748B)) // Slate
            else -> SensorComposeVisuals(Icons.Default.Sensors, Color(0xFF10B981)) // Emerald Green
        }
    }

    fun getValueColor(title: String, valueString: String?, aqiSystem: AqiSystem, unit: String? = null): Color {
        val defaultColor = getVisuals(title).color
        val argb = SensorValueColorResolver.resolveColor(
            title = title,
            valueString = valueString,
            unit = unit,
            aqiSystem = aqiSystem,
            defaultColor = android.graphics.Color.argb(
                (defaultColor.alpha * 255).toInt(),
                (defaultColor.red * 255).toInt(),
                (defaultColor.green * 255).toInt(),
                (defaultColor.blue * 255).toInt()
            )
        )
        return Color(argb)
    }
}
