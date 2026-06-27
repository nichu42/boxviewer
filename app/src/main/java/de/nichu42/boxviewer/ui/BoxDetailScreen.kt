package de.nichu42.boxviewer.ui

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
import androidx.compose.ui.platform.LocalConfiguration
import kotlin.time.Duration.Companion.seconds
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import de.nichu42.boxviewer.util.SensorDisplayConverter
import de.nichu42.boxviewer.data.api.Sensor
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxDetailScreen(
    boxId: String,
    viewModel: SenseBoxViewModel,
    onBack: () -> Unit,
    onNavigateToDashboardWithConfig: (String) -> Unit = {}
) {
    // Select the box on launch and refresh periodically while the screen is at least STARTED
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(boxId, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                viewModel.selectBox(boxId)
                kotlinx.coroutines.delay(60.seconds)
            }
        }
    }

    val selectedBox by viewModel.selectedBox.collectAsStateWithLifecycle()
    val savedBoxes by viewModel.savedBoxes.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    var resolvedLocation by remember { mutableStateOf("") }
    val latitude = selectedBox?.currentLocation?.latitude
    val longitude = selectedBox?.currentLocation?.longitude
    LaunchedEffect(boxId, latitude, longitude) {
        if (latitude != null && longitude != null) {
            viewModel.getCityStateCountryFullFromLocation(boxId, latitude, longitude) { loc ->
                resolvedLocation = loc
            }
        }
    }

    val savedBox = remember(savedBoxes, selectedBox) {
        val box = selectedBox
        if (box != null) {
            savedBoxes.find { it.boxId == box.id }
        } else {
            null
        }
    }

    val isFavorite = savedBox != null

    var showShareQrDialog by remember { mutableStateOf(false) }

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
                                contentDescription = "Sync Box",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { showShareQrDialog = true },
                            modifier = Modifier.testTag("share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.toggleFavorite(box)
                                if (!isFavorite) {
                                    onNavigateToDashboardWithConfig(box.id)
                                }
                            },
                            modifier = Modifier.testTag("favorite_button")
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark Toggle",
                                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
                viewModel.clearDetailScreenCache()
                viewModel.selectBox(boxId, force = true)
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
                        // Collect the synthesized sensor list (includes virtual AQI if PM sensor present)
                        val synthesizedSensors by viewModel.getCachedSensorsFlow(box.id)
                            .collectAsStateWithLifecycle(initialValue = emptyList())
                        val virtualAqiSensor: Sensor? = remember(synthesizedSensors) {
                            synthesizedSensors.firstOrNull { it.sensorId == "virtual_aqi" }?.let { entity ->
                                Sensor(
                                    id = entity.sensorId,
                                    title = entity.sensorTitle,
                                    unit = entity.sensorUnit,
                                    sensorType = entity.sensorType,
                                    lastMeasurement = de.nichu42.boxviewer.data.api.Measurement(
                                        value = entity.value,
                                        createdAt = entity.updatedAt
                                    )
                                )
                            }
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
                                        modifier = Modifier.padding(top = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = "Last Updated Icon",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = lastUpdatedStr,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        val cachedSensors by viewModel.cachedSensors.collectAsStateWithLifecycle()
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Sync,
                                                contentDescription = "App Synced",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            val syncedTime = remember(cachedSensors) {
                                                "Synced: ${viewModel.formatAppSyncTime(cachedSensors)}"
                                            }
                                            Text(
                                                text = syncedTime,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                fontSize = 11.sp
                                            )
                                        }
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

                                    if (!box.grouptag.isNullOrEmpty()) {
                                        Row(
                                            modifier = Modifier.padding(top = 10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            SuggestionChip(
                                                onClick = {},
                                                label = { Text("Group: ${box.grouptag}") },
                                                enabled = false
                                            )
                                        }
                                    }
                                }
                            }

                            // FAVORITE CTA FOR ONBOARDING
                            if (!isFavorite) {
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("add_to_favorites_cta")
                                            .clickable {
                                                selectedBox?.let { box ->
                                                    viewModel.toggleFavorite(box)
                                                    onNavigateToDashboardWithConfig(box.id)
                                                }
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.BookmarkAdd,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text(
                                                    text = "Add to Dashboard",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Text(
                                                    text = "Bookmark this station to save it to your dashboard and show it in your home screen widgets.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
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
                                        if (resolvedLocation.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = resolvedLocation,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
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
                                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, url.toUri())
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("open_opensensemap_button"),
                                                contentPadding = PaddingValues(horizontal = 8.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Language,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "OpenSenseMap",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                            }

                                            // Open in generic map app
                                            OutlinedButton(
                                                onClick = {
                                                    try {
                                                        val latitude = box.currentLocation?.latitude
                                                        val longitude = box.currentLocation?.longitude
                                                        if (latitude != null && longitude != null) {
                                                            val uri = "geo:$latitude,$longitude?q=$latitude,$longitude(${android.net.Uri.encode(box.name)})"
                                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri.toUri())
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
                                    text = "ACTIVE SENSORS (${(box.sensors?.size ?: 0) + (if (virtualAqiSensor != null) 1 else 0)})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            // 4. SENSORS DETAIL CARDS
                            // Canonical sort: Temperature → Humidity → PM10 → PM2.5 → AQI → Pressure → Wind → other
                            val sensorList = ((box.sensors?.filter { !it.id.isNullOrEmpty() } ?: emptyList()) +
                                listOfNotNull(virtualAqiSensor))
                                .sortedWith(compareBy({ de.nichu42.boxviewer.util.SensorSortKey.of(it.title) }, { it.title }))
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

                            // 5. WIDGET ONBOARDING INFOBOX
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Home Screen Widgets",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "To monitor this station on your home screen, go to your home screen, long-press an empty space, select 'Widgets', and look for BoxViewer. You can then select this senseBox in the widget configuration.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
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

    if (showShareQrDialog) {
        selectedBox?.let { box ->
            ShareQrDialog(
                box = box,
                onDismiss = { showShareQrDialog = false }
            )
        }
    }
}
    }
}

@Composable
fun SensorCard(sensor: Sensor, boxId: String, viewModel: SenseBoxViewModel) {
    val temperatureUnit by viewModel.temperatureUnit.collectAsStateWithLifecycle()
    val pressureUnit by viewModel.pressureUnit.collectAsStateWithLifecycle()
    val windUnit by viewModel.windUnit.collectAsStateWithLifecycle()
    val formatPressure by viewModel.formatPressure.collectAsStateWithLifecycle()
    val aqiSystem by viewModel.aqiSystem.collectAsStateWithLifecycle()
    // Expansion and history state are ViewModel-backed so they survive LazyColumn recycling
    // and back-and-forth navigation within the same box detail session.
    val expandedIds by viewModel.expandedSensorIds.collectAsStateWithLifecycle()
    val isExpanded = sensor.id in expandedIds

    val historyCache by viewModel.sensorHistoryCache.collectAsStateWithLifecycle()
    val historyLoadingIds by viewModel.sensorHistoryLoading.collectAsStateWithLifecycle()
    val histCacheKey = "$boxId/${sensor.id}"
    val historicalMeasurements = historyCache[histCacheKey]
    val isLoadingHistory = histCacheKey in historyLoadingIds
    // historyError is surfaced via isLoadingHistory staying false and historicalMeasurements staying null
    val historyError: String? = null // errors are swallowed in ViewModel; sparkline stays empty

    // Trigger history load when card is first expanded (idempotent: no-ops if already loaded/loading)
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            val sensorId = sensor.id ?: return@LaunchedEffect
            val limit = if (sensorId == "virtual_aqi") 150 else 30
            viewModel.loadSensorHistoryIfNeeded(boxId, sensorId, limit)
        }
    }

    val sensorId = sensor.id ?: return
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sensor_detail_card_${sensorId}")
            .clickable { viewModel.toggleSensorExpanded(sensorId) },
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
                val visuals = de.nichu42.boxviewer.ui.theme.SensorTheme.getVisuals(sensor.title)
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
                        text = if (sensor.id == "virtual_aqi") "Locally computed" else "Hardware: ${sensor.sensorType ?: "Generic Sensor"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Updated: ${formatRelativeTime(sensor.lastMeasurement?.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                val conversion = SensorDisplayConverter.convert(
                    rawValue = sensor.lastMeasurement?.value,
                    sourceUnit = sensor.unit,
                    temperatureUnit = temperatureUnit,
                    pressureUnit = pressureUnit,
                    windUnit = windUnit,
                    formatPressure = formatPressure
                )
                val displayVal = conversion.value ?: "--"
                val displayUnit = conversion.unit ?: ""
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = displayVal,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge,
                        color = de.nichu42.boxviewer.ui.theme.SensorTheme.getValueColor(sensor.title, sensor.lastMeasurement?.value, aqiSystem, sensor.unit)
                    )
                    Text(
                        text = displayUnit,
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
                        val rawMeasurements = historicalMeasurements ?: listOf(
                            de.nichu42.boxviewer.data.api.Measurement(
                                value = sensor.lastMeasurement?.value,
                                createdAt = sensor.lastMeasurement?.createdAt
                            )
                        )
                        val conversion = SensorDisplayConverter.convert(
                            rawValue = null,
                            sourceUnit = sensor.unit,
                            temperatureUnit = temperatureUnit,
                            pressureUnit = pressureUnit,
                            windUnit = windUnit,
                            formatPressure = formatPressure
                        )
                        val displayUnit = conversion.unit
                        val displayMeasurements = remember(rawMeasurements, temperatureUnit, pressureUnit, windUnit, formatPressure, aqiSystem) {
                            if (sensor.id == "virtual_aqi") {
                                rawMeasurements.map { m ->
                                    val rawVal = m.value?.toDoubleOrNull()
                                    val res = viewModel.calculateInstantCastForBox(boxId, rawVal)
                                    val displayVal = if (res.value != null) {
                                        String.format(java.util.Locale.US, "%.0f", res.value)
                                    } else {
                                        // For qualitative EU EAQI, map to index severity level for the Sparkline height logic
                                        when (res.label) {
                                            "Very Good" -> "1.0"
                                            "Good", "Satisfactory", "Low Risk (1)", "Low Risk (2)", "Low Risk (3)" -> "2.0"
                                            "Fair", "Moderate", "Moderate Risk (4)", "Moderate Risk (5)", "Moderate Risk (6)", "Moderately Polluted" -> "3.0"
                                            "Poor", "Unhealthy for Sensitive Groups", "High Risk (7)", "High Risk (8)", "High Risk (9)", "High Risk (10)" -> "4.0"
                                            "Very Poor", "Unhealthy", "Very Unhealthy", "Heavily Polluted", "Very High Risk (10+)" -> "5.0"
                                            "Extremely Poor", "Hazardous", "Severe", "Severely Polluted" -> "6.0"
                                            else -> "0.0"
                                        }
                                    }
                                    m.copy(value = displayVal)
                                }
                            } else {
                                rawMeasurements.map { m ->
                                    val historyConversion = SensorDisplayConverter.convert(
                                        rawValue = m.value,
                                        sourceUnit = sensor.unit,
                                        temperatureUnit = temperatureUnit,
                                        pressureUnit = pressureUnit,
                                        windUnit = windUnit,
                                        formatPressure = formatPressure
                                    )
                                    m.copy(value = historyConversion.value)
                                }
                            }
                        }
                        SparklineWithScales(measurements = displayMeasurements, unit = displayUnit)
                        
                        if (sensor.id == "virtual_aqi") {
                            val nowCastResult = remember(rawMeasurements, aqiSystem) {
                                val rawVals = rawMeasurements.mapNotNull { it.value?.toDoubleOrNull() }
                                viewModel.calculateNowCastForBox(boxId, rawVals)
                            }
                            if (nowCastResult.isAvailable) {
                                val scoreText = if (nowCastResult.value != null) {
                                    String.format(java.util.Locale.US, "%.0f", nowCastResult.value)
                                } else {
                                    ""
                                }
                                val badgeColor = Color(android.graphics.Color.parseColor(nowCastResult.colorHex))
                                val textContrastColor = de.nichu42.boxviewer.ui.theme.SensorTheme.getContrastColor(badgeColor)

                                Spacer(modifier = Modifier.height(8.dp))
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(badgeColor, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "AQI NowCast",
                                                fontWeight = FontWeight.SemiBold,
                                                color = textContrastColor.copy(alpha = 0.85f),
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        if (scoreText.isNotEmpty()) {
                                            Text(
                                                text = scoreText,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = textContrastColor,
                                                style = MaterialTheme.typography.headlineMedium
                                            )
                                        }
                                        Text(
                                            text = nowCastResult.label,
                                            fontWeight = FontWeight.Bold,
                                            color = textContrastColor,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "12-hour weighted average · ${aqiSystem.label}",
                                            color = textContrastColor.copy(alpha = 0.7f),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
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
    measurements: List<de.nichu42.boxviewer.data.api.Measurement>,
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
    val locale = LocalConfiguration.current.locales[0]
    val maxLabel = String.format(locale, "%.1f", maxVal) + unitStr
    val minLabel = String.format(locale, "%.1f", minVal) + unitStr

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
                val midLabel = String.format(locale, "%.1f", midVal) + unitStr
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
                text = startTime.ifEmpty { "Oldest" },
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
                text = endTime.ifEmpty { "Latest" },
                fontSize = 9.sp,
                color = textColor,
                fontWeight = FontWeight.Medium
            )
        }
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
    } catch (_: Exception) {
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
    } catch (_: Exception) {
        isoString
    }
}

fun formatRelativeTime(isoString: String?): String {
    if (isoString.isNullOrEmpty()) return "Never updated"
    return try {
        val cleanString = if (isoString.length >= 19) isoString.substring(0, 19) else isoString
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = true
        }
        val date = inputFormat.parse(cleanString) ?: return "Never updated"
        val diffMs = System.currentTimeMillis() - date.time
        if (diffMs < 0) return "Just now"
        
        val diffSec = diffMs / 1000L
        if (diffSec < 60) return "Just now"
        
        val diffMin = diffSec / 60L
        if (diffMin < 60) return "$diffMin min ago"
        
        val diffHours = diffMin / 60L
        if (diffHours < 24) return "$diffHours hours ago"
        
        val diffDays = diffHours / 24L
        if (diffDays < 7) return "$diffDays days ago"
        
        val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        outputFormat.format(date)
    } catch (_: Exception) {
        "Unknown"
    }
}

@Composable
private fun titleLabelStyle() = androidx.compose.ui.text.TextStyle(
    fontWeight = FontWeight.Bold,
    fontSize = 10.sp,
    color = MaterialTheme.colorScheme.primary
)


