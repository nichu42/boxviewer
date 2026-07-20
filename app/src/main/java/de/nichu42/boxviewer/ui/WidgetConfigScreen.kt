package de.nichu42.boxviewer.ui

import android.appwidget.AppWidgetManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.nichu42.boxviewer.R
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Brush
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.roundToInt
import de.nichu42.boxviewer.data.db.SavedBoxEntity
import de.nichu42.boxviewer.data.db.SensorCacheEntity
import de.nichu42.boxviewer.data.db.WidgetConfigEntity
import de.nichu42.boxviewer.data.repository.SenseBoxRepository
import de.nichu42.boxviewer.widget.SenseBoxWidgetProvider
import de.nichu42.boxviewer.util.AqiSystem
import de.nichu42.boxviewer.util.AqiCalculator
import kotlinx.coroutines.launch
import androidx.compose.runtime.key

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(
    repository: SenseBoxRepository,
    appWidgetId: Int,
    onConfigSaved: () -> Unit,
    onConfigCancelled: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val widgetBgColors = listOf(
        Color(0xFF0F172A), // Slate Dark
        Color(0xFF020617), // Deep Navy
        Color(0xFF064E3B), // Forest Green
        Color(0xFF0F3D5C), // Celestial Blue
        Color(0xFF581C87), // Royal Purple
        Color(0xFFF8FAFC), // Slate Light
        Color(0xFFECFDF5), // Mint Green Light
        Color(0xFFF0F9FF), // Sky Blue Light
        Color(0xFFFFFBEB), // Warm Cream Light
        Color(0xFF18181B)  // Dark Charcoal
    )

    var savedBoxes by remember { mutableStateOf<List<SavedBoxEntity>>(emptyList()) }
    var selectedBox by remember { mutableStateOf<SavedBoxEntity?>(null) }
    var availableSensors by remember { mutableStateOf<List<SensorCacheEntity>>(emptyList()) }
    var selectedSensorIds by remember { mutableStateOf<List<String>>(emptyList()) }

    var visualizationType by remember { mutableStateOf("LIST") } // "LIST" or "GRID" (Metric Highlight)
    var widgetColor by remember { mutableStateOf(Color(0xFF0F172A)) }
    var hexInputText by remember { mutableStateOf("0F172A") }
    var hexError by remember { mutableStateOf(false) }
    var isCustomColorExpanded by remember { mutableStateOf(false) }
    var refreshIntervalMinutes by remember { mutableIntStateOf(30) }
    var textScale by remember { mutableFloatStateOf(1.0f) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var boxDropdownExpanded by remember { mutableStateOf(false) }
    var showAqiInfoDialog by remember { mutableStateOf(false) }
    var displayStyleDropdownExpanded by remember { mutableStateOf(false) }
    
    var metricDisplayMode by remember { mutableStateOf("LABEL_VALUE_UNIT") }
    var aqiDisplayMode by remember { mutableStateOf("NUMBER_AND_LABEL") }
    var showRefreshButton by remember { mutableStateOf(true) }
    var showConfigButton by remember { mutableStateOf(true) }
    var showBoxName by remember { mutableStateOf(true) }
    var showUpdateTime by remember { mutableStateOf(true) }
    var useConditionalFormatting by remember { mutableStateOf(true) }
    
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Load existing config and user's saved boxes on launch
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val list = repository.getSavedBoxesList().sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            savedBoxes = list
            
            val existingConfig = repository.getWidgetConfig(appWidgetId)
            if (existingConfig != null) {
                val box = list.find { it.boxId == existingConfig.boxId } ?: list.firstOrNull()
                selectedBox = box
                visualizationType = existingConfig.visualizationType
                
                val savedColorVal = existingConfig.themeColorIndex
                widgetColor = if (savedColorVal in 0..9) {
                    widgetBgColors.getOrElse(savedColorVal) { Color(0xFF0F172A) }
                } else {
                    Color(savedColorVal)
                }
                
                val argb = widgetColor.toArgb()
                hexInputText = String.format("%06X", (argb and 0xFFFFFF))
                
                refreshIntervalMinutes = existingConfig.refreshIntervalMinutes
                textScale = existingConfig.textScale
                selectedSensorIds = existingConfig.sensorIdsString.split(",").filter { it.isNotEmpty() }
                metricDisplayMode = existingConfig.metricDisplayMode
                aqiDisplayMode = existingConfig.aqiDisplayMode
                showRefreshButton = existingConfig.showRefreshButton
                showConfigButton = existingConfig.showConfigButton
                showBoxName = existingConfig.showBoxName
                showUpdateTime = existingConfig.showUpdateTime
                useConditionalFormatting = existingConfig.useConditionalFormatting
            } else if (list.isNotEmpty()) {
                selectedBox = list.first()
                widgetColor = Color(0xFF0F172A)
                hexInputText = "0F172A"
                
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val info = appWidgetManager.getAppWidgetInfo(appWidgetId)
                val providerClass = info?.provider?.className ?: ""
                visualizationType = if (providerClass.contains("Small")) {
                    "GRID"
                } else {
                    "LIST"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    // Load sensors whenever selected box changes
    LaunchedEffect(selectedBox) {
        val box = selectedBox ?: return@LaunchedEffect
        isLoading = true
        statusMessage = context.getString(R.string.widget_status_loading_sensors)
        try {
            // First fetch latest from api to be accurate
            try {
                repository.fetchAndSyncBox(box.boxId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val sensors = repository.getCachedSensors(box.boxId)
            val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            val aqiStr = prefs.getString("aqi_system", AqiSystem.US_EPA.name) ?: AqiSystem.US_EPA.name
            val system = try { AqiSystem.valueOf(aqiStr) } catch (e: Exception) { AqiSystem.US_EPA }
            val synthesized = AqiCalculator.synthesizeVirtualSensors(sensors, system, box.boxId)
                .sortedWith(compareBy({ de.nichu42.boxviewer.util.SensorSortKey.of(it.sensorTitle) }, { it.sensorTitle }))
            availableSensors = synthesized

            val existingConfig = repository.getWidgetConfig(appWidgetId)
            selectedSensorIds = if (existingConfig != null && existingConfig.boxId == box.boxId) {
                existingConfig.sensorIdsString.split(",").filter { it.isNotEmpty() }
            } else {
                // Default to the top 6 canonical-order sensors (includes AQI when PM data is present)
                synthesized.take(6).map { it.sensorId }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
            statusMessage = null
        }
    }

    val hsvValues = remember(widgetColor) {
        val arr = FloatArray(3)
        android.graphics.Color.colorToHSV(widgetColor.toArgb(), arr)
        arr
    }
    val currentHue = hsvValues[0]         // 0f .. 360f
    val currentSaturation = hsvValues[1]  // 0f .. 1f
    val currentBrightness = hsvValues[2]  // 0f .. 1f

    val colorFromHsv = { h: Float, s: Float, v: Float ->
        Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)))
    }

    val onColorFromWheel = { h: Float, s: Float ->
        // Keep current brightness, update hue/saturation, preserve alpha
        val newColor = colorFromHsv(h, s, currentBrightness).copy(alpha = widgetColor.alpha)
        widgetColor = newColor
        hexInputText = String.format("%06X", (newColor.toArgb() and 0xFFFFFF))
        hexError = false
    }

    val onBrightnessChanged = { b: Float ->
        // Keep current hue/saturation, update brightness, preserve alpha
        val newColor = colorFromHsv(currentHue, currentSaturation, b).copy(alpha = widgetColor.alpha)
        widgetColor = newColor
        hexInputText = String.format("%06X", (newColor.toArgb() and 0xFFFFFF))
        hexError = false
    }

    val onAlphaChanged = { a: Float ->
        widgetColor = widgetColor.copy(alpha = a)
    }

    val onHexChanged = { input: String ->
        val cleanInput = input.trim().filter { it.isLetterOrDigit() }.take(6)
        hexInputText = cleanInput
        
        hexError = !(cleanInput.length == 6 && run {
            val r = cleanInput.substring(0, 2).toIntOrNull(16)
            val g = cleanInput.substring(2, 4).toIntOrNull(16)
            val b = cleanInput.substring(4, 6).toIntOrNull(16)
            if (r != null && g != null && b != null) {
                // Preserve current alpha
                widgetColor = Color(r, g, b).copy(alpha = widgetColor.alpha)
                true
            } else {
                false
            }
        })
    }

    val onPresetSelected = { presetColor: Color ->
        // Preserve current alpha on preset
        val newColor = presetColor.copy(alpha = widgetColor.alpha)
        widgetColor = newColor
        val argb = newColor.toArgb()
        hexInputText = String.format("%06X", (argb and 0xFFFFFF))
        hexError = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.widget_config_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onConfigCancelled) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_cancel))
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
        ) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                

                // CHOOSE SENSEBOX (Data Selection)
                item {
                    Text(
                        stringResource(R.string.widget_config_section_sensebox),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    if (savedBoxes.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Text(
                                stringResource(R.string.widget_config_no_saved_boxes),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { boxDropdownExpanded = true }
                                    .testTag("sensebox_selector_dropdown"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Sensors,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = selectedBox?.name ?: stringResource(R.string.widget_config_select_sensebox_hint),
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = stringResource(R.string.widget_config_exposure, selectedBox?.exposure ?: "outdoor"),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = if (boxDropdownExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = stringResource(R.string.cd_expand_dropdown),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = boxDropdownExpanded,
                                onDismissRequest = { boxDropdownExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                savedBoxes.forEach { box ->
                                    val isSelected = selectedBox?.boxId == box.boxId
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Sensors,
                                                        contentDescription = null,
                                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = box.name,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = stringResource(R.string.widget_config_exposure, box.exposure ?: "outdoor"),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = stringResource(R.string.cd_selected),
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedBox = box
                                            boxDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // VISUALIZATION FORMAT (Visual Formats & Styling)
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                    Text(
                        stringResource(R.string.widget_config_section_visualization),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .clickable { visualizationType = "LIST" },
                            colors = CardDefaults.cardColors(
                                containerColor = if (visualizationType == "LIST") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(stringResource(R.string.widget_config_visualization_list), style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .clickable {
                                    visualizationType = "GRID"
                                    if (selectedSensorIds.size > 1) {
                                        selectedSensorIds = listOf(selectedSensorIds.first())
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.widget_config_grid_warning),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else if (selectedSensorIds.isEmpty() && availableSensors.isNotEmpty()) {
                                        selectedSensorIds = listOf(availableSensors.first().sensorId)
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (visualizationType == "GRID") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(stringResource(R.string.widget_config_visualization_grid), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                // MONITORED METRICS / HIGHLIGHT METRIC (Data Selection)
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                    Text(
                        if (visualizationType == "GRID") stringResource(R.string.widget_config_section_highlight_metric) else stringResource(R.string.widget_config_section_monitored_metrics),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    if (availableSensors.isEmpty()) {
                        Text(
                            stringResource(R.string.widget_config_select_sensebox_prompt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (visualizationType == "GRID") {
                        val currentSelectedSensor = availableSensors.find { selectedSensorIds.contains(it.sensorId) } ?: availableSensors.firstOrNull()
                        if (currentSelectedSensor != null && !selectedSensorIds.contains(currentSelectedSensor.sensorId)) {
                            LaunchedEffect(currentSelectedSensor) {
                                selectedSensorIds = listOf(currentSelectedSensor.sensorId)
                            }
                        }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { dropdownExpanded = true }
                                    .testTag("single_highlight_sensor_dropdown"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        val visuals = de.nichu42.boxviewer.ui.theme.SensorTheme.getVisuals(currentSelectedSensor?.sensorTitle ?: "")
                                        Icon(
                                            imageVector = visuals.icon,
                                            contentDescription = null,
                                            tint = visuals.color,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = currentSelectedSensor?.sensorTitle ?: stringResource(R.string.widget_config_choose_sensor),
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = visuals.color
                                            )
                                            if (currentSelectedSensor != null) {
                                                Text(
                                                    text = if (currentSelectedSensor.sensorId == "virtual_aqi") stringResource(R.string.sensor_locally_computed) else stringResource(R.string.widget_config_sensor_subtitle, currentSelectedSensor.sensorUnit ?: "", currentSelectedSensor.sensorType ?: ""),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    Icon(
                                        imageVector = if (dropdownExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = stringResource(R.string.cd_expand_dropdown),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                availableSensors.forEach { sensor ->
                                    val isSelected = currentSelectedSensor?.sensorId == sensor.sensorId
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    val sVisuals = de.nichu42.boxviewer.ui.theme.SensorTheme.getVisuals(sensor.sensorTitle)
                                                    Icon(
                                                        imageVector = sVisuals.icon,
                                                        contentDescription = null,
                                                        tint = sVisuals.color,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = sensor.sensorTitle,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSelected) sVisuals.color else MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = if (sensor.sensorId == "virtual_aqi") stringResource(R.string.sensor_locally_computed) else stringResource(R.string.widget_config_sensor_subtitle, sensor.sensorUnit ?: "", sensor.sensorType ?: ""),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = stringResource(R.string.cd_selected),
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedSensorIds = listOf(sensor.sensorId)
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Monitored/Active Metrics (Drag and drop sortable list)
                                if (selectedSensorIds.isNotEmpty()) {
                                    Text(
                                        stringResource(R.string.widget_config_section_monitored_metrics_reorder),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    val itemHeightDp = 64.dp
                                    var draggedIndex by remember { mutableStateOf<Int?>(null) }
                                    var dragOffsetY by remember { mutableFloatStateOf(0f) }
                                    val density = LocalDensity.current
                                    val itemHeightPx = with(density) { itemHeightDp.toPx() }
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(itemHeightDp * selectedSensorIds.size)
                                    ) {
                                        selectedSensorIds.forEachIndexed { index, sensorId ->
                                            key(sensorId) {
                                                val sensor = availableSensors.find { it.sensorId == sensorId }
                                                if (sensor != null) {
                                                    val currentIndexState = rememberUpdatedState(index)
                                                    val currentItemHeightPxState = rememberUpdatedState(itemHeightPx)
                                                    val isBeingDragged = draggedIndex == index
                                                    val currentVisualOffset = if (isBeingDragged) dragOffsetY else 0f
                                                    val basePositionDp = itemHeightDp * index
                                                    
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(56.dp)
                                                            .offset(
                                                                x = 0.dp,
                                                                y = basePositionDp + with(density) { currentVisualOffset.toDp() }
                                                            )
                                                            .zIndex(if (isBeingDragged) 10f else 1f)
                                                            .background(
                                                                color = if (isBeingDragged) {
                                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                                                                } else {
                                                                    MaterialTheme.colorScheme.surface
                                                                },
                                                                shape = RoundedCornerShape(12.dp)
                                                            )
                                                            .border(
                                                                width = 1.dp,
                                                                color = if (isBeingDragged) {
                                                                    MaterialTheme.colorScheme.primary
                                                                } else {
                                                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                                                },
                                                                shape = RoundedCornerShape(12.dp)
                                                            )
                                                            .padding(horizontal = 12.dp),
                                                        contentAlignment = Alignment.CenterStart
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier.weight(1f)
                                                            ) {
                                                                Checkbox(
                                                                    checked = true,
                                                                    onCheckedChange = { isChecked ->
                                                                        if (!isChecked) {
                                                                            selectedSensorIds = selectedSensorIds - sensorId
                                                                        }
                                                                    }
                                                                )
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                val sVisuals = de.nichu42.boxviewer.ui.theme.SensorTheme.getVisuals(sensor.sensorTitle)
                                                                Icon(
                                                                    imageVector = sVisuals.icon,
                                                                    contentDescription = null,
                                                                    tint = sVisuals.color,
                                                                    modifier = Modifier.size(20.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                Column {
                                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                                        Text(
                                                                            text = sensor.sensorTitle,
                                                                            style = MaterialTheme.typography.bodyMedium,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = sVisuals.color
                                                                        )
                                                                        if (sensor.sensorId == "virtual_aqi") {
                                                                            Spacer(modifier = Modifier.width(4.dp))
                                                                            IconButton(
                                                                                onClick = { showAqiInfoDialog = true },
                                                                                modifier = Modifier.size(24.dp)
                                                                            ) {
                                                                                Icon(
                                                                                    imageVector = Icons.Default.Info,
                                                                                    contentDescription = stringResource(R.string.cd_aqi_info),
                                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                                    modifier = Modifier.size(16.dp)
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                    Text(
                                                                        text = if (sensor.sensorId == "virtual_aqi") stringResource(R.string.sensor_locally_computed) else stringResource(R.string.widget_config_sensor_subtitle, sensor.sensorUnit ?: "", sensor.sensorType ?: ""),
                                                                        style = MaterialTheme.typography.bodySmall,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                    )
                                                                }
                                                            }
                                                            
                                                            Icon(
                                                                imageVector = Icons.Default.Reorder,
                                                                contentDescription = stringResource(R.string.cd_drag_to_reorder),
                                                                tint = if (isBeingDragged) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier
                                                                    .size(28.dp)
                                                                    .pointerInput(sensorId) {
                                                                        detectDragGesturesAfterLongPress(
                                                                            onDragStart = { _ ->
                                                                                draggedIndex = currentIndexState.value
                                                                                dragOffsetY = 0f
                                                                            },
                                                                            onDragEnd = {
                                                                                draggedIndex = null
                                                                                dragOffsetY = 0f
                                                                            },
                                                                            onDragCancel = {
                                                                                draggedIndex = null
                                                                                dragOffsetY = 0f
                                                                            },
                                                                            onDrag = { change, dragAmount ->
                                                                                change.consume()
                                                                                dragOffsetY += dragAmount.y
                                                                                
                                                                                draggedIndex?.let { currentIdx ->
                                                                                    if (selectedSensorIds.isNotEmpty()) {
                                                                                        val targetIdx = (currentIdx + (dragOffsetY / currentItemHeightPxState.value).roundToInt())
                                                                                            .coerceIn(0, selectedSensorIds.size - 1)
                                                                                        if (targetIdx != currentIdx) {
                                                                                            val newList = selectedSensorIds.toMutableList()
                                                                                            if (currentIdx in newList.indices && targetIdx in newList.indices) {
                                                                                                val movedItem = newList.removeAt(currentIdx)
                                                                                                newList.add(targetIdx, movedItem)
                                                                                                selectedSensorIds = newList
                                                                                                draggedIndex = targetIdx
                                                                                                dragOffsetY -= (targetIdx - currentIdx) * currentItemHeightPxState.value
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        )
                                                                    }
                                                                    .padding(4.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                } else {
                                    Text(
                                        stringResource(R.string.widget_config_no_metrics_selected),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                val unselectedSensors = availableSensors.filter { !selectedSensorIds.contains(it.sensorId) }
                                if (unselectedSensors.isNotEmpty()) {
                                    if (selectedSensorIds.isNotEmpty()) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    }
                                    
                                    Text(
                                        stringResource(R.string.widget_config_section_add_metrics),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        unselectedSensors.forEach { sensor ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        if (selectedSensorIds.size < 6) {
                                                            selectedSensorIds = selectedSensorIds + sensor.sensorId
                                                        }
                                                    }
                                                    .padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = false,
                                                    onCheckedChange = { isChecked ->
                                                        if (isChecked && selectedSensorIds.size < 6) {
                                                            selectedSensorIds = selectedSensorIds + sensor.sensorId
                                                        }
                                                    }
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                val sVisuals = de.nichu42.boxviewer.ui.theme.SensorTheme.getVisuals(sensor.sensorTitle)
                                                Icon(
                                                    imageVector = sVisuals.icon,
                                                    contentDescription = null,
                                                    tint = sVisuals.color,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = sensor.sensorTitle,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = sVisuals.color
                                                        )
                                                        if (sensor.sensorId == "virtual_aqi") {
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            IconButton(
                                                                onClick = { showAqiInfoDialog = true },
                                                                modifier = Modifier.size(24.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Info,
                                                                    contentDescription = stringResource(R.string.cd_aqi_info),
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Text(
                                                        text = if (sensor.sensorId == "virtual_aqi") stringResource(R.string.sensor_locally_computed) else stringResource(R.string.widget_config_sensor_subtitle, sensor.sensorUnit ?: "", sensor.sensorType ?: ""),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // METRIC DISPLAY STYLE (Visual Formats & Styling)
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                    Text(
                        stringResource(R.string.widget_config_section_metric_display_style),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        stringResource(R.string.widget_config_metric_display_style_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { displayStyleDropdownExpanded = true }
                                .testTag("metric_display_style_dropdown"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val currentLabel = when (metricDisplayMode) {
                                    "LABEL_VALUE_UNIT" -> stringResource(R.string.widget_display_style_full_details)
                                    "VALUE_UNIT" -> stringResource(R.string.widget_display_style_value_unit)
                                    "VALUE_ONLY" -> stringResource(R.string.widget_display_style_value_only)
                                    else -> stringResource(R.string.widget_display_style_full_details)
                                }
                                Text(
                                    text = currentLabel,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = if (displayStyleDropdownExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = stringResource(R.string.cd_expand_dropdown),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = displayStyleDropdownExpanded,
                            onDismissRequest = { displayStyleDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            listOf(
                                "LABEL_VALUE_UNIT" to stringResource(R.string.widget_display_style_full_details),
                                "VALUE_UNIT" to stringResource(R.string.widget_display_style_value_unit),
                                "VALUE_ONLY" to stringResource(R.string.widget_display_style_value_only)
                            ).forEach { (mode, label) ->
                                val isSelected = metricDisplayMode == mode
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = stringResource(R.string.cd_selected),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        metricDisplayMode = mode
                                        displayStyleDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.widget_config_conditional_formatting_label), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(stringResource(R.string.widget_config_conditional_formatting_description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = useConditionalFormatting,
                                onCheckedChange = { useConditionalFormatting = it }
                            )
                        }
                    }
                }

                // AQI VALUE DISPLAY (Visual Formats & Styling)
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                    Text(
                        stringResource(R.string.widget_config_section_aqi_display),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        stringResource(R.string.widget_config_aqi_display_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "NUMBER_AND_LABEL" to stringResource(R.string.widget_aqi_display_number_and_label),
                            "NUMBER_ONLY" to stringResource(R.string.widget_aqi_display_number_only),
                            "LABEL_ONLY" to stringResource(R.string.widget_aqi_display_label_only)
                        ).forEach { (mode, label) ->
                            val isSelected = aqiDisplayMode == mode
                            OutlinedCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(80.dp)
                                    .clickable { aqiDisplayMode = mode },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // WIDGET TEXT SCALING (Visual Formats & Styling)
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                    Text(
                        stringResource(R.string.widget_config_section_text_scaling),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        stringResource(R.string.widget_config_text_scaling_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.widget_config_text_scale_label),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TextFormat,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format(java.util.Locale.US, "%.0f%%", textScale * 100),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Slider(
                            value = textScale,
                            onValueChange = { textScale = it },
                            valueRange = 0.6f..2.0f,
                            modifier = Modifier.fillMaxWidth().testTag("widget_text_scale_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                stringResource(R.string.widget_config_text_scale_compact),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                stringResource(R.string.widget_config_text_scale_default),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                stringResource(R.string.widget_config_text_scale_double),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // WIDGET BACKGROUND COLOR (UI Elements)
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                    Text(
                        stringResource(R.string.widget_config_section_background_color),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                stringResource(R.string.widget_config_select_preset_palette),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                widgetBgColors.forEach { color ->
                                    val isSelected = widgetColor == color
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(color)
                                            .clickable { onPresetSelected(color) }
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            val isLightPreset = (color.red * 0.2126 + color.green * 0.7152 + color.blue * 0.0722) > 0.5
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = stringResource(R.string.cd_selected),
                                                tint = if (isLightPreset) Color.Black else Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isCustomColorExpanded = !isCustomColorExpanded }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.widget_config_advanced_settings),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(
                                    imageVector = if (isCustomColorExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isCustomColorExpanded) stringResource(R.string.cd_collapse) else stringResource(R.string.cd_expand),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            if (isCustomColorExpanded) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
                                
                                Text(
                                    stringResource(R.string.widget_config_color_wheel),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Interactive Circle Color Wheel
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    var wheelSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
                                    val currentOnColorFromWheelState = rememberUpdatedState(onColorFromWheel)
                                    
                                    Canvas(
                                        modifier = Modifier
                                            .size(170.dp)
                                            .onSizeChanged { wheelSize = it }
                                            .pointerInput(Unit) {
                                                detectDragGestures(
                                                    onDragStart = { offset ->
                                                        val cw = wheelSize.width
                                                        val ch = wheelSize.height
                                                        if (cw > 0 && ch > 0) {
                                                            val cx = cw / 2f
                                                            val cy = ch / 2f
                                                            val r = cw.coerceAtMost(ch) / 2f
                                                            val dx = offset.x - cx
                                                            val dy = offset.y - cy
                                                            val dist = sqrt(dx * dx + dy * dy).coerceAtMost(r)
                                                            var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                                            if (angleDeg < 0) angleDeg += 360f
                                                            val sat = dist / r
                                                            currentOnColorFromWheelState.value(angleDeg, sat)
                                                        }
                                                    },
                                                    onDrag = { change, _ ->
                                                        val cw = wheelSize.width
                                                        val ch = wheelSize.height
                                                        if (cw > 0 && ch > 0) {
                                                            val cx = cw / 2f
                                                            val cy = ch / 2f
                                                            val r = cw.coerceAtMost(ch) / 2f
                                                            val dx = change.position.x - cx
                                                            val dy = change.position.y - cy
                                                            val dist = sqrt(dx * dx + dy * dy).coerceAtMost(r)
                                                            var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                                            if (angleDeg < 0) angleDeg += 360f
                                                            val sat = dist / r
                                                            currentOnColorFromWheelState.value(angleDeg, sat)
                                                        }
                                                    }
                                                )
                                            }
                                    ) {
                                        val cx = size.width / 2f
                                        val cy = size.height / 2f
                                        val r = size.width.coerceAtMost(size.height) / 2f

                                        // 1. Draw Sweep Gradient Hue Wheel
                                        val sweepGradient = Brush.sweepGradient(
                                            colors = listOf(
                                                Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                                            ),
                                            center = androidx.compose.ui.geometry.Offset(cx, cy)
                                        )
                                        drawCircle(brush = sweepGradient, radius = r, center = androidx.compose.ui.geometry.Offset(cx, cy))

                                        // 2. Draw Radial Gradient of White to transparent to blend Saturation
                                        val radialGradient = Brush.radialGradient(
                                            colors = listOf(Color.White, Color.Transparent),
                                            center = androidx.compose.ui.geometry.Offset(cx, cy),
                                            radius = r
                                        )
                                        drawCircle(brush = radialGradient, radius = r, center = androidx.compose.ui.geometry.Offset(cx, cy))

                                        // 3. Draw Selector Handle
                                        val angleRad = Math.toRadians(currentHue.toDouble())
                                        val handleDist = currentSaturation * r
                                        val handleX = cx + handleDist * cos(angleRad).toFloat()
                                        val handleY = cy + handleDist * sin(angleRad).toFloat()

                                        // Outer handle indicator ring (shadow / dark)
                                        drawCircle(
                                            color = Color(0x33000000),
                                            radius = 14.dp.toPx(),
                                            center = androidx.compose.ui.geometry.Offset(handleX, handleY)
                                        )
                                        drawCircle(
                                            color = Color.Black,
                                            radius = 11.dp.toPx(),
                                            center = androidx.compose.ui.geometry.Offset(handleX, handleY),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                                        )
                                        // Inner white ring so it's super visible against all backgrounds
                                        drawCircle(
                                            color = Color.White,
                                            radius = 9.dp.toPx(),
                                            center = androidx.compose.ui.geometry.Offset(handleX, handleY),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                        )
                                        // Center of handle: filled with the source color at full brightness (makes drag selection clear)
                                        val previewColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(currentHue, currentSaturation, 1f)))
                                        drawCircle(
                                            color = previewColor,
                                            radius = 7.dp.toPx(),
                                            center = androidx.compose.ui.geometry.Offset(handleX, handleY)
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                                Text(
                                    stringResource(R.string.widget_config_brightness_value),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Brightness Value Slider representation
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BrightnessLow,
                                        contentDescription = stringResource(R.string.cd_low_brightness),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    
                                    Slider(
                                        value = currentBrightness,
                                        onValueChange = onBrightnessChanged,
                                        valueRange = 0f..1f,
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    
                                    Icon(
                                        imageVector = Icons.Default.BrightnessHigh,
                                        contentDescription = stringResource(R.string.cd_high_brightness),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    
                                    Text(
                                        text = stringResource(R.string.widget_config_percentage, (currentBrightness * 100).toInt()),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.width(36.dp)
                                    )
                                }
                                
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                                
                                Text(
                                    stringResource(R.string.widget_config_transparency),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Widget Transparency Slider
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.InvertColors,
                                        contentDescription = stringResource(R.string.cd_opaque),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    
                                    Slider(
                                        value = 1f - widgetColor.alpha,
                                        onValueChange = { transparency ->
                                            onAlphaChanged(1f - transparency)
                                        },
                                        valueRange = 0f..1f,
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    
                                    Icon(
                                        imageVector = Icons.Default.InvertColors,
                                        contentDescription = stringResource(R.string.cd_transparent),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    
                                    Text(
                                        text = stringResource(R.string.widget_config_percentage, ((1f - widgetColor.alpha) * 100).toInt()),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.width(36.dp)
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            stringResource(R.string.widget_config_hex_color_code),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedTextField(
                                            value = hexInputText,
                                            onValueChange = onHexChanged,
                                            modifier = Modifier.fillMaxWidth(),
                                            placeholder = { Text(stringResource(R.string.widget_config_hex_placeholder), color = de.nichu42.boxviewer.ui.theme.SensorTheme.getContrastColor(widgetColor).copy(alpha = 0.6f)) },
                                            leadingIcon = {
                                                Text(
                                                    "#",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = de.nichu42.boxviewer.ui.theme.SensorTheme.getContrastColor(widgetColor)
                                                )
                                            },
                                            singleLine = true,
                                            isError = hexError,
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = de.nichu42.boxviewer.ui.theme.SensorTheme.getContrastColor(widgetColor)
                                            ),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = widgetColor,
                                                unfocusedContainerColor = widgetColor,
                                                disabledContainerColor = widgetColor,
                                                errorContainerColor = widgetColor,
                                                focusedTextColor = de.nichu42.boxviewer.ui.theme.SensorTheme.getContrastColor(widgetColor),
                                                unfocusedTextColor = de.nichu42.boxviewer.ui.theme.SensorTheme.getContrastColor(widgetColor),
                                                focusedPlaceholderColor = de.nichu42.boxviewer.ui.theme.SensorTheme.getContrastColor(widgetColor).copy(alpha = 0.6f),
                                                unfocusedPlaceholderColor = de.nichu42.boxviewer.ui.theme.SensorTheme.getContrastColor(widgetColor).copy(alpha = 0.6f),
                                                focusedBorderColor = if (hexError) MaterialTheme.colorScheme.error else de.nichu42.boxviewer.ui.theme.SensorTheme.getContrastColor(widgetColor),
                                                unfocusedBorderColor = if (hexError) MaterialTheme.colorScheme.error else de.nichu42.boxviewer.ui.theme.SensorTheme.getContrastColor(widgetColor).copy(alpha = 0.5f)
                                            )
                                        )
                                        if (hexError) {
                                            Text(
                                                stringResource(R.string.widget_config_hex_error),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // WIDGET HEADER LABELS (UI Elements)
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                    Text(
                        stringResource(R.string.widget_config_section_header_labels),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.widget_config_show_box_name), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(stringResource(R.string.widget_config_show_box_name_description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = showBoxName,
                                    onCheckedChange = { showBoxName = it }
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.widget_config_show_update_time), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(stringResource(R.string.widget_config_show_update_time_description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = showUpdateTime,
                                    onCheckedChange = { showUpdateTime = it }
                                )
                            }
                        }
                    }
                }

                // WIDGET HEADER BUTTONS (UI Elements)
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                    Text(
                        stringResource(R.string.widget_config_section_header_buttons),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.widget_config_show_refresh_button), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(stringResource(R.string.widget_config_show_refresh_button_description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = showRefreshButton,
                                    onCheckedChange = { showRefreshButton = it }
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.widget_config_show_config_button), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(stringResource(R.string.widget_config_show_config_button_description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = showConfigButton,
                                    onCheckedChange = { showConfigButton = it }
                                )
                            }
                        }
                    }
                }

                // REFRESH SYNC TIME INTERVAL (Background Frequency)
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                    Text(
                        stringResource(R.string.widget_config_section_refresh_interval),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        stringResource(R.string.widget_config_refresh_interval_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.widget_config_refresh_interval_label),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.widget_config_refresh_interval_minutes, refreshIntervalMinutes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Slider(
                            value = refreshIntervalMinutes.toFloat(),
                            onValueChange = { refreshIntervalMinutes = it.roundToInt() },
                            valueRange = 5f..60f,
                            steps = 10,
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                stringResource(R.string.widget_config_refresh_interval_fast),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                stringResource(R.string.widget_config_refresh_interval_eco),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ACTION CONTROL BUTTONS
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = onConfigCancelled,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.action_discard))
                        }

                        Button(
                            onClick = {
                                val b = selectedBox
                                if (b == null) {
                                    statusMessage = context.getString(R.string.widget_status_select_sensebox)
                                    return@Button
                                }
                                coroutineScope.launch {
                                    isLoading = true
                                    statusMessage = context.getString(R.string.widget_status_provisioning)
                                    try {
                                        val entity = WidgetConfigEntity(
                                            widgetId = appWidgetId,
                                            boxId = b.boxId,
                                            boxName = b.name,
                                            sensorIdsString = selectedSensorIds.joinToString(","),
                                            refreshIntervalMinutes = refreshIntervalMinutes,
                                            visualizationType = visualizationType,
                                            themeColorIndex = widgetColor.toArgb(),
                                            lastFetchedTime = System.currentTimeMillis(),
                                            textScale = textScale,
                                            metricDisplayMode = metricDisplayMode,
                                            aqiDisplayMode = aqiDisplayMode,
                                            showRefreshButton = showRefreshButton,
                                            showConfigButton = showConfigButton,
                                            showBoxName = showBoxName,
                                            showUpdateTime = showUpdateTime,
                                            useConditionalFormatting = useConditionalFormatting
                                        )
                                        repository.saveWidgetConfig(entity)
                                        
                                        // Request dynamic broadcast trigger from provider to redraw instantly!
                                        val appWidgetManager = AppWidgetManager.getInstance(context)
                                        SenseBoxWidgetProvider.updateWidgetAsync(context, appWidgetManager, appWidgetId)
                                        SenseBoxWidgetProvider.scheduleAlarm(context, appWidgetId, refreshIntervalMinutes)
                                        
                                        onConfigSaved()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        statusMessage = context.getString(R.string.widget_status_save_failed, e.message ?: "")
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_config_button"),
                            enabled = selectedBox != null && !isLoading
                        ) {
                            Text(stringResource(R.string.action_apply_widget))
                        }
                    }
                }
            }
        }

        if (showAqiInfoDialog) {
            AlertDialog(
                onDismissRequest = { showAqiInfoDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.widget_config_aqi_guide_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.widget_config_aqi_guide_intro),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.widget_config_aqi_guide_instantcast),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = stringResource(R.string.widget_config_aqi_guide_nowcast),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = stringResource(R.string.widget_config_aqi_guide_consolidated),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.widget_config_aqi_guide_customize),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAqiInfoDialog = false }) {
                        Text(stringResource(R.string.action_got_it))
                    }
                }
            )
        }
    }
}
