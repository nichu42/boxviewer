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
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.toColorInt
import de.nichu42.boxviewer.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AqiInfoScreen(
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.aqi_info_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("aqi_info_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
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
                        text = stringResource(R.string.aqi_info_intro_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.aqi_info_intro_description),
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
                        text = stringResource(R.string.aqi_info_cast_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = stringResource(R.string.aqi_info_instantcast_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.aqi_info_instantcast_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = stringResource(R.string.aqi_info_nowcast_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.aqi_info_nowcast_description),
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
                        text = stringResource(R.string.aqi_info_virtual_sensor_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.aqi_info_virtual_sensor_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Standards Header
            Text(
                text = stringResource(R.string.aqi_info_regional_title),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )

            // US EPA
            AqiStandardDetailCard(
                name = stringResource(R.string.aqi_system_us_epa),
                scale = stringResource(R.string.aqi_scale_us),
                bands = listOf(
                    AqiBandInfo(stringResource(R.string.aqi_band_good), "0–50", "#00E400"),
                    AqiBandInfo(stringResource(R.string.aqi_band_moderate), "51–100", "#FFFF00", darkText = true),
                    AqiBandInfo(stringResource(R.string.aqi_band_sensitive_groups), "101–150", "#FF7E00"),
                    AqiBandInfo(stringResource(R.string.aqi_band_unhealthy), "151–200", "#FF0000"),
                    AqiBandInfo(stringResource(R.string.aqi_band_very_unhealthy), "201–300", "#8F3F97"),
                    AqiBandInfo(stringResource(R.string.aqi_band_hazardous), "301–500", "#7E0023")
                )
            )

            // UK DAQI
            AqiStandardDetailCard(
                name = stringResource(R.string.aqi_system_uk_daqi),
                scale = stringResource(R.string.aqi_scale_uk),
                bands = listOf(
                    AqiBandInfo(stringResource(R.string.aqi_band_low), "1–3", "#008000"),
                    AqiBandInfo(stringResource(R.string.aqi_band_moderate), "4–6", "#FFFF00", darkText = true),
                    AqiBandInfo(stringResource(R.string.aqi_band_high), "7–9", "#FF0000"),
                    AqiBandInfo(stringResource(R.string.aqi_band_very_high), "10", "#800080")
                )
            )

            // European EAQI
            AqiStandardDetailCard(
                name = stringResource(R.string.aqi_system_eu_eaqi),
                scale = stringResource(R.string.aqi_scale_eu),
                bands = listOf(
                    AqiBandInfo(stringResource(R.string.aqi_band_very_good), "—", "#5AAA5F"),
                    AqiBandInfo(stringResource(R.string.aqi_band_good), "—", "#A7D25C", darkText = true),
                    AqiBandInfo(stringResource(R.string.aqi_band_moderate), "—", "#ECD347", darkText = true),
                    AqiBandInfo(stringResource(R.string.aqi_band_poor), "—", "#EF9A3C"),
                    AqiBandInfo(stringResource(R.string.aqi_band_very_poor), "—", "#E8665E"),
                    AqiBandInfo(stringResource(R.string.aqi_band_extremely_poor), "—", "#B765A2")
                )
            )

            // Canada AQHI
            AqiStandardDetailCard(
                name = stringResource(R.string.aqi_system_canada_aqhi),
                scale = stringResource(R.string.aqi_scale_canada),
                bands = listOf(
                    AqiBandInfo(stringResource(R.string.aqi_band_low_risk), "1–3", "#00E5FF", darkText = true),
                    AqiBandInfo(stringResource(R.string.aqi_band_moderate_risk), "4–6", "#FBC02D", darkText = true),
                    AqiBandInfo(stringResource(R.string.aqi_band_high_risk), "7–10", "#E53935"),
                    AqiBandInfo(stringResource(R.string.aqi_band_very_high_risk), "10+", "#8E24AA")
                )
            )

            // India AQI
            AqiStandardDetailCard(
                name = stringResource(R.string.aqi_system_india_aqi),
                scale = stringResource(R.string.aqi_scale_india),
                bands = listOf(
                    AqiBandInfo(stringResource(R.string.aqi_band_good), "0–50", "#4CAF50"),
                    AqiBandInfo(stringResource(R.string.aqi_band_satisfactory), "51–100", "#8BC34A"),
                    AqiBandInfo(stringResource(R.string.aqi_band_moderately_polluted), "101–200", "#FFEB3B", darkText = true),
                    AqiBandInfo(stringResource(R.string.aqi_band_poor), "201–300", "#FF9800"),
                    AqiBandInfo(stringResource(R.string.aqi_band_very_poor), "301–400", "#F44336"),
                    AqiBandInfo(stringResource(R.string.aqi_band_severe), "401–500", "#B71C1C")
                )
            )

            // China AQI
            AqiStandardDetailCard(
                name = stringResource(R.string.aqi_system_china_aqi),
                scale = stringResource(R.string.aqi_scale_china),
                bands = listOf(
                    AqiBandInfo(stringResource(R.string.aqi_band_excellent), "0–50", "#00E400"),
                    AqiBandInfo(stringResource(R.string.aqi_band_good), "51–100", "#FFFF00", darkText = true),
                    AqiBandInfo(stringResource(R.string.aqi_band_lightly_polluted), "101–150", "#FF7E00"),
                    AqiBandInfo(stringResource(R.string.aqi_band_moderately_polluted), "151–200", "#FF0000"),
                    AqiBandInfo(stringResource(R.string.aqi_band_heavily_polluted), "201–300", "#8F3F97"),
                    AqiBandInfo(stringResource(R.string.aqi_band_severely_polluted), "301–500", "#7E0023")
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
                            .background(Color(band.hexColor.toColorInt()))
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
