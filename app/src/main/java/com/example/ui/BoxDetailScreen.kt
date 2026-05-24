package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.border
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.api.Sensor
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxDetailScreen(
    boxId: String,
    viewModel: SenseBoxViewModel,
    onBack: () -> Unit,
    onNavigateToDashboardWithConfig: (String) -> Unit = {}
) {
    // Select the box in viewModel on launch
    LaunchedEffect(boxId) {
        viewModel.selectBox(boxId)
    }

    val selectedBox by viewModel.selectedBox.collectAsStateWithLifecycle()
    val savedBoxes by viewModel.savedBoxes.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    val savedBox = remember(savedBoxes, selectedBox) {
        val box = selectedBox
        if (box != null) {
            savedBoxes.find { it.boxId == box.id }
        } else {
            null
        }
    }

    val isFavorite = savedBox != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("senseBox Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    selectedBox?.let { box ->
                        IconButton(
                            onClick = { viewModel.selectBox(box.id) },
                            modifier = Modifier.testTag("sync_box_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync Box"
                            )
                        }
                        IconButton(
                            onClick = {
                                val wasFavorite = isFavorite
                                viewModel.toggleFavorite(box)
                                if (!wasFavorite) {
                                    onNavigateToDashboardWithConfig(box.id)
                                }
                            },
                            modifier = Modifier.testTag("favorite_button")
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark Toggle",
                                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isLoading && selectedBox != null,
            onRefresh = {
                viewModel.selectBox(boxId)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (isLoading && selectedBox == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    selectedBox?.let { box ->
                        val lastUpdatedStr = remember(box) {
                            viewModel.formatLastUpdated(box)
                        }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("detail_scroll_view"),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            
                            // 1. HEADER SECTION
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = box.name,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = box.exposure ?: "outdoor",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = "Last Updated Icon",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = lastUpdatedStr,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    box.description?.let { desc ->
                                        if (desc.trim().isNotEmpty()) {
                                            Text(
                                                text = desc,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(top = 8.dp)
                                            )
                                        }
                                    }

                                    val prettyModel = remember(box.model) { getPrettyModelName(box.model) }
                                    if (!prettyModel.isNullOrEmpty() || !box.grouptag.isNullOrEmpty()) {
                                        Row(
                                            modifier = Modifier.padding(top = 10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (!prettyModel.isNullOrEmpty()) {
                                                SuggestionChip(
                                                    onClick = {},
                                                    label = { Text("Model: $prettyModel") },
                                                    enabled = false
                                                )
                                            }
                                            if (!box.grouptag.isNullOrEmpty()) {
                                                SuggestionChip(
                                                    onClick = {},
                                                    label = { Text("Group: ${box.grouptag}") },
                                                    enabled = false
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. LOCATION COORDINATES CARD
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                    ) {
                                        Text(
                                            "DEVICE LOCATION & LINK",
                                            style = titleLabelStyle()
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    "Latitude: " + box.currentLocation?.latitude.toString(),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    "Longitude: " + box.currentLocation?.longitude.toString(),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Place,
                                                    contentDescription = "Map Location",
                                                    tint = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Open on OpenSenseMap (Web URL)
                                            Button(
                                                onClick = {
                                                    try {
                                                        val url = "https://opensensemap.org/explore/${box.id}"
                                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("open_opensensemap_button"),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Language,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("OpenSenseMap", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }

                                            // Open in generic map app
                                            OutlinedButton(
                                                onClick = {
                                                    try {
                                                        val latitude = box.currentLocation?.latitude
                                                        val longitude = box.currentLocation?.longitude
                                                        if (latitude != null && longitude != null) {
                                                            val uri = "geo:$latitude,$longitude?q=$latitude,$longitude(${android.net.Uri.encode(box.name)})"
                                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uri))
                                                            context.startActivity(intent)
                                                        }
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("open_maps_button"),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Map,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("View Map", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. SENSORS SECTION HEADER
                            item {
                                Text(
                                    text = "ACTIVE SENSORS (${box.sensors?.size ?: 0})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            // 4. SENSORS DETAIL CARDS
                            val sensorList = box.sensors ?: emptyList()
                            if (sensorList.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "No sensor configuration data available.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                items(sensorList) { sensor ->
                                    SensorCard(sensor = sensor, boxId = box.id, viewModel = viewModel)
                                }
                            }
                        }
                    } ?: run {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = errorMessage ?: "No senseBox Selected",
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SensorCard(sensor: Sensor, boxId: String, viewModel: SenseBoxViewModel) {
    var isExpanded by remember { mutableStateOf(false) }

    var historicalMeasurements by remember { mutableStateOf<List<com.example.data.api.Measurement>?>(null) }
    var isLoadingHistory by remember { mutableStateOf(false) }
    var historyError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isExpanded) {
        if (isExpanded && historicalMeasurements == null) {
            isLoadingHistory = true
            historyError = null
            try {
                // Request 30 historical measurements for higher resolution grid as requested
                val measurements = viewModel.getSensorData(boxId, sensor.id, limit = 30)
                // Reverse list of measurements to place them chronologically oldest to newest (left-to-right)
                historicalMeasurements = measurements.reversed()
            } catch (e: Exception) {
                e.printStackTrace()
                historyError = "Failed to load real history: ${e.localizedMessage}"
                // Generate chronological fallback measurements
                val baseVal = sensor.lastMeasurement?.value?.toDoubleOrNull() ?: 15.0
                historicalMeasurements = List(6) { index ->
                    com.example.data.api.Measurement(
                        value = (baseVal + Random(sensor.id.hashCode() + index).nextDouble() * 2.0 - 1.0).toString(),
                        createdAt = null
                    )
                }
            } finally {
                isLoadingHistory = false
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sensor_detail_card_${sensor.id}")
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Appropriate icon based on Sensor Title
                val visuals = com.example.ui.theme.SensorTheme.getVisuals(sensor.title)
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(visuals.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = visuals.icon,
                        contentDescription = sensor.title,
                        tint = visuals.color
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sensor.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Hardware: ${sensor.sensorType ?: "Generic Sensor"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = sensor.lastMeasurement?.value ?: "--",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge,
                        color = com.example.ui.theme.SensorTheme.getValueColor(sensor.title, sensor.lastMeasurement?.value)
                    )
                    Text(
                        text = sensor.unit ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    val labelText = if (isLoadingHistory) {
                        "FETCHING HISTORICAL MEASUREMENT TREND..."
                    } else if (historyError != null) {
                        "HISTORICAL MEASUREMENT TREND (FALLBACK DATA)"
                    } else {
                        "HISTORICAL MEASUREMENT TREND"
                    }
                    Text(
                        labelText,
                        style = titleLabelStyle()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (isLoadingHistory) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(94.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        val measurements = historicalMeasurements ?: listOf(
                            com.example.data.api.Measurement(
                                value = sensor.lastMeasurement?.value,
                                createdAt = sensor.lastMeasurement?.createdAt
                            )
                        )
                        SparklineWithScales(measurements = measurements, unit = sensor.unit)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Last Measurement Recorded:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val formattedDate = remember(sensor.lastMeasurement?.createdAt) {
                            formatIsoTimestamp(sensor.lastMeasurement?.createdAt)
                        }
                        Text(
                            text = formattedDate,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Beautiful, fully formatted visual chart with scales and grid dashed markings as required.
 */
@Composable
fun SparklineWithScales(
    measurements: List<com.example.data.api.Measurement>,
    unit: String?
) {
    if (measurements.isEmpty()) return

    val cleanData = remember(measurements) {
        measurements.mapNotNull { it.value?.toDoubleOrNull() }
    }

    if (cleanData.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No numerical data points recorded.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxVal = cleanData.maxOrNull() ?: 1.0
    val minVal = cleanData.minOrNull() ?: 0.0
    val delta = if (maxVal == minVal) 1.0 else maxVal - minVal

    // Construct Axis Scales
    val unitStr = if (unit != null) " $unit" else ""
    val maxLabel = String.format(Locale.getDefault(), "%.1f", maxVal) + unitStr
    val minLabel = String.format(Locale.getDefault(), "%.1f", minVal) + unitStr

    val startTime = remember(measurements) {
        formatShortTime(measurements.firstOrNull()?.createdAt)
    }
    val endTime = remember(measurements) {
        formatShortTime(measurements.lastOrNull()?.createdAt)
    }

    val strokeColor = MaterialTheme.colorScheme.primary
    val areaColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(95.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chart Area occupies 80% of width, Y-Axis labels take 20%
            Canvas(
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxHeight()
            ) {
                val lineStyle = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                // Top dashed grid line
                drawLine(
                    color = gridColor,
                    start = Offset(0f, 4f),
                    end = Offset(size.width, 4f),
                    pathEffect = lineStyle.pathEffect
                )

                // Mid dashed grid line
                drawLine(
                    color = gridColor,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    pathEffect = lineStyle.pathEffect
                )

                // Bottom dashed grid line
                drawLine(
                    color = gridColor,
                    start = Offset(0f, size.height - 4f),
                    end = Offset(size.width, size.height - 4f),
                    pathEffect = lineStyle.pathEffect
                )

                if (cleanData.size > 1) {
                    val points = cleanData.mapIndexed { idx, value ->
                        val x = (idx.toFloat() / (cleanData.size - 1)) * size.width
                        // Keep a 4px cushion at the top and bottom of the canvas so line does not touch borders
                        val y = 4f + ((size.height - 8f) - (((value - minVal) / delta) * (size.height - 8f)).toFloat())
                        Offset(x, y)
                    }

                    // Linear area gradient fill below path
                    val areaPath = Path().apply {
                        moveTo(0f, size.height)
                        points.forEach { pt ->
                            lineTo(pt.x, pt.y)
                        }
                        lineTo(size.width, size.height)
                        close()
                    }
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(areaColor, Color.Transparent)
                        )
                    )

                    // Line stroke
                    val linePath = Path().apply {
                        points.getOrNull(0)?.let { moveTo(it.x, it.y) }
                        points.forEach { pt ->
                            lineTo(pt.x, pt.y)
                        }
                    }
                    drawPath(
                        path = linePath,
                        color = strokeColor,
                        style = Stroke(width = 2.dp.toPx())
                    )
                } else {
                    // Constant line
                    val halfHeight = size.height / 2
                    drawLine(
                        color = strokeColor,
                        start = Offset(0f, halfHeight),
                        end = Offset(size.width, halfHeight),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Y-Axis Labels
            Column(
                modifier = Modifier
                    .weight(0.2f)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = maxLabel,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1
                )
                val midVal = minVal + delta / 2
                val midLabel = String.format(Locale.getDefault(), "%.1f", midVal) + unitStr
                Text(
                    text = midLabel,
                    fontSize = 8.sp,
                    color = textColor.copy(alpha = 0.55f),
                    maxLines = 1
                )
                Text(
                    text = minLabel,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // X-Axis bounds (Earliest left aligned, latest right aligned, point count center aligned)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 45.dp), // compensate for the offset Y column space
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (startTime.isNotEmpty()) startTime else "Oldest",
                fontSize = 9.sp,
                color = textColor,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${cleanData.size} measurements",
                fontSize = 8.sp,
                color = textColor.copy(alpha = 0.5f),
                fontWeight = FontWeight.Normal
            )
            Text(
                text = if (endTime.isNotEmpty()) endTime else "Latest",
                fontSize = 9.sp,
                color = textColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Native Sparkline Canvas drawing for inline trending charts.
 */
@Composable
fun Sparkline(data: List<Double>) {
    val strokeColor = MaterialTheme.colorScheme.primary
    val areaColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
    ) {
        if (data.size <= 1) return@Canvas

        val max = data.maxOrNull() ?: 1.0
        val min = data.minOrNull() ?: 0.0
        val delta = if (max == min) 1.0 else max - min

        val points = data.mapIndexed { idx, value ->
            val x = (idx.toFloat() / (data.size - 1)) * size.width
            val y = size.height - (((value - min) / delta) * size.height).toFloat()
            Offset(x, y)
        }

        // Draw area path first
        val areaPath = Path().apply {
            moveTo(0f, size.height)
            points.forEach { points ->
                lineTo(points.x, points.y)
            }
            lineTo(size.width, size.height)
            close()
        }
        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(areaColor, Color.Transparent)
            )
        )

        // Draw line path
        val linePath = Path().apply {
            points.getOrNull(0)?.let { moveTo(it.x, it.y) }
            points.forEach { points ->
                lineTo(points.x, points.y)
            }
        }
        drawPath(
            path = linePath,
            color = strokeColor,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

fun formatShortTime(isoString: String?): String {
    if (isoString.isNullOrEmpty()) return ""
    return try {
        val cleanString = if (isoString.length >= 19) isoString.substring(0, 19) else isoString
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = true
        }
        val date = inputFormat.parse(cleanString)
        val outputFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
        if (date != null) outputFormat.format(date) else ""
    } catch (e: Exception) {
        ""
    }
}



fun formatIsoTimestamp(isoString: String?): String {
    if (isoString.isNullOrEmpty()) return "Unknown"
    return try {
        // Safe substring extraction
        val cleanString = if (isoString.length >= 19) isoString.substring(0, 19) else isoString
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = true
        }
        val date = inputFormat.parse(cleanString)
        val outputFormat = SimpleDateFormat("MMM d, yyyy - HH:mm", Locale.getDefault())
        if (date != null) outputFormat.format(date) else isoString
    } catch (e: Exception) {
        isoString
    }
}

@Composable
private fun titleLabelStyle() = androidx.compose.ui.text.TextStyle(
    fontWeight = FontWeight.Bold,
    fontSize = 10.sp,
    color = MaterialTheme.colorScheme.primary
)

private fun getPrettyModelName(modelKey: String?): String? {
    if (modelKey.isNullOrBlank()) return null
    return when (val lower = modelKey.lowercase()) {
        "homeethernet" -> "senseBox:home (Ethernet)"
        "homewifi" -> "senseBox:home (WiFi)"
        "home" -> "senseBox:home"
        "custom" -> "Custom senseBox"
        "eduethernet" -> "senseBox:edu (Ethernet)"
        "eduwifi" -> "senseBox:edu (WiFi)"
        "edu" -> "senseBox:edu"
        "mcu" -> "senseBox MCU"
        "bike" -> "senseBox:bike"
        "luftdaten" -> "Luftdaten"
        else -> modelKey.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}
