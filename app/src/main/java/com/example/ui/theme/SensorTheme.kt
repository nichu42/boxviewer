package com.example.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

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
            lower.contains("pm10") || lower.contains("pm2") || lower.contains("air") || lower.contains("dust") || lower.contains("feinstaub") -> SensorComposeVisuals(Icons.Default.Air, Color(0xFFEC4899)) // Pink
            lower.contains("cop") || lower.contains("co2") || lower.contains("carbon") -> SensorComposeVisuals(Icons.Default.Co2, Color(0xFF34D399)) // Emerald/Mint
            lower.contains("bell") || lower.contains("light") || lower.contains("lux") || lower.contains("sonne") -> SensorComposeVisuals(Icons.Default.WbSunny, Color(0xFFF59E0B)) // Yellow/Amber
            lower.contains("uv") -> SensorComposeVisuals(Icons.Default.LightMode, Color(0xFFF59E0B)) // Yellow/Amber
            lower.contains("speed") || lower.contains("wind") -> SensorComposeVisuals(Icons.Default.Speed, Color(0xFF64748B)) // Slate
            else -> SensorComposeVisuals(Icons.Default.Sensors, Color(0xFF10B981)) // Emerald Green
        }
    }

    fun getValueColor(title: String, valueString: String?): Color {
        val value = valueString?.toDoubleOrNull() ?: return getVisuals(title).color
        val lower = title.lowercase()
        return when {
            lower.contains("temp") -> {
                when {
                    value <= 0.0 -> Color(0xFF1E88E5)      // Freezing: Deep Blue
                    value <= 15.0 -> Color(0xFF64B5F6)     // Cold: Light Blue
                    value <= 25.0 -> Color(0xFF4CAF50)     // Comfortable: Green
                    value <= 32.0 -> Color(0xFFFFA726)     // Warm: Orange
                    else -> Color(0xFFE53935)              // Hot: Red
                }
            }
            lower.contains("feucht") || lower.contains("humid") -> {
                when {
                    value < 30.0 -> Color(0xFFFFB74D)      // Low/Dry: Amber
                    value <= 60.0 -> Color(0xFF4CAF50)     // Optimal: Green
                    else -> Color(0xFF0288D1)              // High/Sticky: Moisture Blue
                }
            }
            lower.contains("druck") || lower.contains("press") -> {
                when {
                    value < 1000.0 -> Color(0xFF90A4AE)    // Low/Stormy: Stormy Gray
                    value <= 1020.0 -> Color(0xFF4CAF50)   // Normal/Stable: Green
                    else -> Color(0xFFFBC02D)              // High/Dry: Bright Yellow
                }
            }
            lower.contains("pm10") -> {
                when {
                    value <= 45.0 -> Color(0xFF4CAF50)     // Good: Green
                    value <= 60.0 -> Color(0xFFFBC02D)     // Fair: Yellow
                    value <= 100.0 -> Color(0xFFFFA726)    // Poor: Orange
                    else -> Color(0xFFE53935)              // Severe: Red
                }
            }
            lower.contains("pm2") || lower.contains("pm 2") -> {
                when {
                    value <= 15.0 -> Color(0xFF4CAF50)     // Good: Green
                    value <= 25.0 -> Color(0xFFFBC02D)     // Fair: Yellow
                    value <= 50.0 -> Color(0xFFFFA726)     // Poor: Orange
                    else -> Color(0xFFE53935)              // Severe: Red
                }
            }
            else -> getVisuals(title).color
        }
    }
}
