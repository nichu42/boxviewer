package de.nichu42.boxviewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AqiInfoScreen(
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Air Quality Index Guide", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("aqi_info_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Intro Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Understanding Air Quality Index (AQI)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The Air Quality Index is a standardized system used to report daily air quality and associate it with public health risks. BoxViewer synthesizes a local 'virtual' AQI sensor if a station publishes PM2.5 or PM10 particulate matter readings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Calculation Methods Card
            Card(
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "InstantCast vs. NowCast",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "InstantCast (Live Readings)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Directly translates the most recent single measurement value using standard breakpoints. Used on the main dashboard list and widgets for real-time responsiveness.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "NowCast (Historical Analysis)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Uses the EPA's 12-hour weighted average algorithm. If air quality is changing rapidly, it puts more weight on recent hours; if stable, it weights them more evenly. Calculated and displayed when expanding detailed sensor cards.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Virtual Sensor Card
            Card(
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Consolidated Virtual Sensor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "If a station contains both PM2.5 (fine particles) and PM10 (coarse particles), BoxViewer calculates the sub-index for both and automatically displays the worst-case (maximum) score. This ensures maximum protection and a clean layout.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Standards Header
            Text(
                text = "REGIONAL AQI SPECIFICATIONS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )

            // US EPA
            AqiStandardDetailCard(
                name = "US EPA AQI (United States)",
                scale = "0 – 500 Scale",
                bands = listOf(
                    AqiBandInfo("Good", "0–50", "#00E400"),
                    AqiBandInfo("Moderate", "51–100", "#FFFF00", darkText = true),
                    AqiBandInfo("Sensitive Groups", "101–150", "#FF7E00"),
                    AqiBandInfo("Unhealthy", "151–200", "#FF0000"),
                    AqiBandInfo("Very Unhealthy", "201–300", "#8F3F97"),
                    AqiBandInfo("Hazardous", "301–500", "#7E0023")
                )
            )

            // UK DAQI
            AqiStandardDetailCard(
                name = "UK DAQI (United Kingdom)",
                scale = "1 – 10 Scale",
                bands = listOf(
                    AqiBandInfo("Low", "1–3", "#008000"),
                    AqiBandInfo("Moderate", "4–6", "#FFFF00", darkText = true),
                    AqiBandInfo("High", "7–9", "#FF0000"),
                    AqiBandInfo("Very High", "10", "#800080")
                )
            )

            // European EAQI
            AqiStandardDetailCard(
                name = "European EAQI (European Union)",
                scale = "Qualitative Bands (No numeric index)",
                bands = listOf(
                    AqiBandInfo("Very Good", "—", "#5AAA5F"),
                    AqiBandInfo("Good", "—", "#A7D25C", darkText = true),
                    AqiBandInfo("Moderate", "—", "#ECD347", darkText = true),
                    AqiBandInfo("Poor", "—", "#EF9A3C"),
                    AqiBandInfo("Very Poor", "—", "#E8665E"),
                    AqiBandInfo("Extremely Poor", "—", "#B765A2")
                )
            )

            // Canada AQHI
            AqiStandardDetailCard(
                name = "Canada AQHI (Canada)",
                scale = "1 – 10+ Scale (PM2.5 only)",
                bands = listOf(
                    AqiBandInfo("Low Risk", "1–3", "#00E5FF", darkText = true),
                    AqiBandInfo("Moderate Risk", "4–6", "#FBC02D", darkText = true),
                    AqiBandInfo("High Risk", "7–10", "#E53935"),
                    AqiBandInfo("Very High Risk", "10+", "#8E24AA")
                )
            )

            // India AQI
            AqiStandardDetailCard(
                name = "India AQI (India)",
                scale = "0 – 500 Scale",
                bands = listOf(
                    AqiBandInfo("Good", "0–50", "#4CAF50"),
                    AqiBandInfo("Satisfactory", "51–100", "#8BC34A"),
                    AqiBandInfo("Moderately Polluted", "101–200", "#FFEB3B", darkText = true),
                    AqiBandInfo("Poor", "201–300", "#FF9800"),
                    AqiBandInfo("Very Poor", "301–400", "#F44336"),
                    AqiBandInfo("Severe", "401–500", "#B71C1C")
                )
            )

            // China AQI
            AqiStandardDetailCard(
                name = "China AQI (China)",
                scale = "0 – 500 Scale",
                bands = listOf(
                    AqiBandInfo("Excellent", "0–50", "#00E400"),
                    AqiBandInfo("Good", "51–100", "#FFFF00", darkText = true),
                    AqiBandInfo("Lightly Polluted", "101–150", "#FF7E00"),
                    AqiBandInfo("Moderately Polluted", "151–200", "#FF0000"),
                    AqiBandInfo("Heavily Polluted", "201–300", "#8F3F97"),
                    AqiBandInfo("Severely Polluted", "301–500", "#7E0023")
                )
            )
        }
    }
}

data class AqiBandInfo(
    val label: String,
    val rangeText: String,
    val hexColor: String,
    val darkText: Boolean = false
)

@Composable
private fun AqiStandardDetailCard(
    name: String,
    scale: String,
    bands: List<AqiBandInfo>
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(text = scale, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                bands.forEach { band ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(android.graphics.Color.parseColor(band.hexColor)))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = band.label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (band.darkText) Color(0xFF0F172A) else Color(0xFFF1F5F9)
                        )
                        if (band.rangeText != "—") {
                            Text(
                                text = band.rangeText,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = if (band.darkText) Color(0xFF0F172A) else Color(0xFFF1F5F9)
                            )
                        }
                    }
                }
            }
        }
    }
}
