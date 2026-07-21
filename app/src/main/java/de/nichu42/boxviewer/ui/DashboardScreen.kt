package de.nichu42.boxviewer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.zIndex
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import de.nichu42.boxviewer.R
import de.nichu42.boxviewer.util.SensorDisplayConverter
import de.nichu42.boxviewer.data.db.SavedBoxEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: SenseBoxViewModel,
    onBoxSelected: (String) -> Unit,
    onGoToDiscovery: () -> Unit
) {
    val savedBoxes by viewModel.savedBoxes.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    var isReorderMode by remember { mutableStateOf(false) }
    var reorderList by remember { mutableStateOf<List<SavedBoxEntity>>(emptyList()) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && viewModel.savedBoxes.value.isNotEmpty()) {
                viewModel.refreshAll(force = false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(isReorderMode, savedBoxes) {
        if (savedBoxes.isEmpty()) {
            isReorderMode = false
        }
        if (isReorderMode) {
            reorderList = savedBoxes
        }
    }

    LaunchedEffect(isReorderMode, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            if (!isReorderMode) {
                while (true) {
                    kotlinx.coroutines.delay(60.seconds)
                    if (viewModel.savedBoxes.value.isNotEmpty()) {
                        viewModel.refreshAll()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.dashboard_title),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("app_title")
                    )
                },
                actions = {
                    if (savedBoxes.isNotEmpty()) {
                        IconButton(
                            onClick = { isReorderMode = !isReorderMode },
                            modifier = Modifier.testTag("toggle_reorder_mode_button")
                        ) {
                            Icon(
                                imageVector = if (isReorderMode) Icons.Default.Check else Icons.Default.SwapVert,
                                contentDescription = stringResource(
                                    if (isReorderMode) R.string.cd_save_order else R.string.cd_reorder_boxes
                                ),
                                tint = if (isReorderMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { viewModel.refreshAll(force = true) },
                            modifier = Modifier.testTag("refresh_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.cd_refresh_all),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
            isRefreshing = isLoading && !isReorderMode,
            onRefresh = { viewModel.refreshAll(force = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                AnimatedVisibility(visible = errorMessage != null) {
                    errorMessage?.let { error ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Row(
                                modifier = Modifier
                                   .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.clearError() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.cd_dismiss_error),
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }

                if (savedBoxes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudQueue,
                                    contentDescription = stringResource(R.string.cd_no_saved_boxes),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.dashboard_empty_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.dashboard_empty_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    onGoToDiscovery()
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(stringResource(R.string.dashboard_empty_button))
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = stringResource(R.string.dashboard_info_title),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.dashboard_info_description_1),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = stringResource(R.string.dashboard_info_description_2),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                } else if (isReorderMode) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.dashboard_reorder_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        val density = LocalDensity.current
                        var draggedIdx by remember { mutableStateOf<Int?>(null) }
                        var dragY by remember { mutableFloatStateOf(0f) }
                        val itemHeightDp = 68.dp // 56.dp + 12.dp spacing
                        val itemHeightPx = with(density) { itemHeightDp.toPx() }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(itemHeightDp * reorderList.size)
                        ) {
                            reorderList.forEachIndexed { index, box ->
                                key(box.boxId) {
                                    val currentIndexState = rememberUpdatedState(index)
                                    val currentItemHeightPxState = rememberUpdatedState(itemHeightPx)
                                    val isBeingDragged = draggedIdx == index
                                    val currentVisualOffset = if (isBeingDragged) dragY else 0f
                                    val basePositionDp = itemHeightDp * index

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .offset {
                                                IntOffset(
                                                    x = 0,
                                                    y = (basePositionDp.toPx() + currentVisualOffset).roundToInt()
                                                )
                                            }
                                            .zIndex(if (isBeingDragged) 10f else 1f)
                                    ) {
                                        ReorderBoxRow(
                                            box = box,
                                            isBeingDragged = isBeingDragged,
                                            modifier = Modifier.pointerInput(box.boxId) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = { _ ->
                                                        draggedIdx = currentIndexState.value
                                                        dragY = 0f
                                                    },
                                                    onDragEnd = {
                                                        viewModel.updateSavedBoxesOrder(reorderList)
                                                        draggedIdx = null
                                                        dragY = 0f
                                                    },
                                                    onDragCancel = {
                                                        viewModel.updateSavedBoxesOrder(reorderList)
                                                        draggedIdx = null
                                                        dragY = 0f
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragY += dragAmount.y

                                                        draggedIdx?.let { currentIdx ->
                                                            if (reorderList.isNotEmpty()) {
                                                                val targetIdx = (currentIdx + (dragY / currentItemHeightPxState.value).roundToInt())
                                                                    .coerceIn(0, reorderList.size - 1)
                                                                if (targetIdx != currentIdx) {
                                                                    val newList = reorderList.toMutableList()
                                                                    if (currentIdx in newList.indices && targetIdx in newList.indices) {
                                                                        val movedItem = newList.removeAt(currentIdx)
                                                                        newList.add(targetIdx, movedItem)
                                                                        reorderList = newList
                                                                        dragY -= (targetIdx - currentIdx) * currentItemHeightPxState.value
                                                                        draggedIdx = targetIdx
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                )
                                            },
                                            onUnfavoriteClick = {
                                                viewModel.unfavoriteBox(box.boxId)
                                                if (reorderList.size <= 1) {
                                                    isReorderMode = false
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // List of saved boxes
                        items(savedBoxes, key = { it.boxId }) { box ->
                            SavedBoxCard(
                                box = box,
                                viewModel = viewModel,
                                onClick = { onBoxSelected(box.boxId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SavedBoxCard(
    box: SavedBoxEntity,
    viewModel: SenseBoxViewModel,
    onClick: () -> Unit
) {
    val cachedSensors by viewModel.getCachedSensorsFlow(box.boxId).collectAsStateWithLifecycle(initialValue = emptyList())
    val autoConfigureBoxId by viewModel.autoConfigureBoxId.collectAsStateWithLifecycle()
    val useConditionalFormatting by viewModel.useConditionalFormatting.collectAsStateWithLifecycle()
    val temperatureUnit by viewModel.temperatureUnit.collectAsStateWithLifecycle()
    val pressureUnit by viewModel.pressureUnit.collectAsStateWithLifecycle()
    val windUnit by viewModel.windUnit.collectAsStateWithLifecycle()
    val formatPressure by viewModel.formatPressure.collectAsStateWithLifecycle()
    val aqiSystem by viewModel.aqiSystem.collectAsStateWithLifecycle()
    var isConfiguring by remember { mutableStateOf(false) }

    val locale = LocalLocale.current.platformLocale
    var resolvedLocation by remember(locale, box.latitude, box.longitude) {
        mutableStateOf(String.format(locale, "Lat: %.3f, Lon: %.3f", box.latitude, box.longitude))
    }
    LaunchedEffect(box.boxId, box.latitude, box.longitude) {
        viewModel.getCityStateCountryFromLocation(box.boxId, box.latitude, box.longitude) { loc ->
            resolvedLocation = loc
        }
    }

    LaunchedEffect(autoConfigureBoxId) {
        if (autoConfigureBoxId == box.boxId) {
            isConfiguring = true
            viewModel.setAutoConfigureBox(null)
        }
    }

    val density = LocalDensity.current
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    val selectedSensorIds = remember(box.dashboardSensorIds) {
        box.dashboardSensorIds?.split(",")?.filter { it.isNotEmpty() }?.distinct() ?: emptyList()
    }

    val sensorsToDisplay = remember(cachedSensors, selectedSensorIds) {
        if (selectedSensorIds.isNotEmpty()) {
            selectedSensorIds.mapNotNull { id -> cachedSensors.find { it.sensorId == id } }
        } else {
            emptyList()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("saved_box_card_${box.boxId}")
            .then(
                if (!isConfiguring) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = box.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = exposureLabel(box.exposure),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(
                        onClick = { isConfiguring = !isConfiguring },
                        modifier = Modifier.size(24.dp).testTag("configure_card_button_${box.boxId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.cd_configure_metrics),
                            tint = if (isConfiguring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            box.description?.let { desc ->
                if (desc.trim().isNotEmpty()) {
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            if (isConfiguring) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                var selectedSensorIdsState by remember(box.dashboardSensorIds) {
                    mutableStateOf(
                        box.dashboardSensorIds?.split(",")?.filter { it.isNotEmpty() }?.distinct() ?: emptyList()
                    )
                }

                val itemHeightDp = 50.dp
                val itemHeightPx = with(density) { itemHeightDp.toPx() }

                // Select/Deselect All option
                if (cachedSensors.isNotEmpty()) {
                    val allSelected = selectedSensorIdsState.size == cachedSensors.size
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val newList = if (allSelected) {
                                    emptyList()
                                } else {
                                    cachedSensors.map { it.sensorId }
                                }
                                selectedSensorIdsState = newList
                                viewModel.updateDashboardSensors(box.boxId, newList)
                            }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = allSelected,
                            onCheckedChange = null,
                            modifier = Modifier.testTag("select_all_metrics_checkbox_${box.boxId}")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                if (allSelected) R.string.dashboard_deselect_all_metrics else R.string.dashboard_select_all_metrics
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 1. DRAGGABLE SELECTED METRICS SECTION
                if (selectedSensorIdsState.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.dashboard_selected_metrics_header),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeightDp * selectedSensorIdsState.size)
                    ) {
                        selectedSensorIdsState.forEachIndexed { index, sensorId ->
                            key(sensorId) {
                                val sensor = cachedSensors.find { it.sensorId == sensorId }
                                if (sensor != null) {
                                    val currentIndexState = rememberUpdatedState(index)
                                    val currentItemHeightPxState = rememberUpdatedState(itemHeightPx)
                                    val isBeingDragged = draggedIndex == index
                                    val currentVisualOffset = if (isBeingDragged) dragOffsetY else 0f
                                    val basePositionDp = itemHeightDp * index

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .offset {
                                                IntOffset(
                                                    x = 0,
                                                    y = (basePositionDp.toPx() + currentVisualOffset).roundToInt()
                                                )
                                            }
                                            .zIndex(if (isBeingDragged) 10f else 1f)
                                            .background(
                                                color = if (isBeingDragged) {
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                                                } else {
                                                    MaterialTheme.colorScheme.surface
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isBeingDragged) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                                },
                                                shape = RoundedCornerShape(8.dp)
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
                                                            val newList = (selectedSensorIdsState - sensorId).distinct()
                                                            selectedSensorIdsState = newList
                                                            viewModel.updateDashboardSensors(box.boxId, newList)
                                                        }
                                                    }
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                val sVisuals = de.nichu42.boxviewer.ui.theme.SensorTheme.getVisuals(sensor.sensorTitle)
                                                Icon(
                                                    imageVector = sVisuals.icon,
                                                    contentDescription = null,
                                                    tint = sVisuals.color,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = sensor.sensorTitle,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = sVisuals.color
                                                )
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
                                                                viewModel.updateDashboardSensors(box.boxId, selectedSensorIdsState)
                                                                draggedIndex = null
                                                                dragOffsetY = 0f
                                                            },
                                                            onDragCancel = {
                                                                viewModel.updateDashboardSensors(box.boxId, selectedSensorIdsState)
                                                                draggedIndex = null
                                                                dragOffsetY = 0f
                                                            },
                                                            onDrag = { change, dragAmount ->
                                                                change.consume()
                                                                dragOffsetY += dragAmount.y

                                                                draggedIndex?.let { currentIdx ->
                                                                    if (selectedSensorIdsState.isNotEmpty()) {
                                                                        val targetIdx = (currentIdx + (dragOffsetY / currentItemHeightPxState.value).roundToInt())
                                                                            .coerceIn(0, selectedSensorIdsState.size - 1)
                                                                        if (targetIdx != currentIdx) {
                                                                            val newList = selectedSensorIdsState.toMutableList()
                                                                            if (currentIdx in newList.indices && targetIdx in newList.indices) {
                                                                                val movedItem = newList.removeAt(currentIdx)
                                                                                newList.add(targetIdx, movedItem)
                                                                                selectedSensorIdsState = newList
                                                                                dragOffsetY -= (targetIdx - currentIdx) * currentItemHeightPxState.value
                                                                                draggedIndex = targetIdx
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
                } else {
                    Text(
                        text = stringResource(R.string.dashboard_no_metrics_selected),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. AVAILABLE UNSELECTED METRICS LIST
                val unselectedSensors = cachedSensors.filter { !selectedSensorIdsState.contains(it.sensorId) }
                if (unselectedSensors.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.dashboard_available_sensors_header),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                     Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        unselectedSensors.forEach { sensor ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        val newList = (selectedSensorIdsState + sensor.sensorId).distinct()
                                        selectedSensorIdsState = newList
                                        viewModel.updateDashboardSensors(box.boxId, newList)
                                    }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = false,
                                    onCheckedChange = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val sVisuals = de.nichu42.boxviewer.ui.theme.SensorTheme.getVisuals(sensor.sensorTitle)
                                Icon(
                                    imageVector = sVisuals.icon,
                                    contentDescription = null,
                                    tint = sVisuals.color,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = sensor.sensorTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    sensor.value?.let { v ->
                                        val conversion = SensorDisplayConverter.convert(
                                            rawValue = v,
                                            sourceUnit = sensor.sensorUnit,
                                            temperatureUnit = temperatureUnit,
                                            pressureUnit = pressureUnit,
                                            windUnit = windUnit,
                                            formatPressure = formatPressure
                                        )
                                        Text(
                                            text = stringResource(
                                                R.string.dashboard_current_value_format,
                                                conversion.value ?: stringResource(R.string.widget_no_value),
                                                conversion.unit ?: ""
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { isConfiguring = false },
                    modifier = Modifier.align(Alignment.End),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.dashboard_done))
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                
                if (sensorsToDisplay.isEmpty()) {
                    Text(
                        text = stringResource(R.string.dashboard_no_metrics_available),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        sensorsToDisplay.forEach { s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val visuals = de.nichu42.boxviewer.ui.theme.SensorTheme.getVisuals(s.sensorTitle)
                                    Icon(
                                        imageVector = visuals.icon,
                                        contentDescription = s.sensorTitle,
                                        tint = visuals.color,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = s.sensorTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                val valueColor = if (useConditionalFormatting) {
                                    de.nichu42.boxviewer.ui.theme.SensorTheme.getValueColor(s.sensorTitle, s.value, aqiSystem, s.sensorUnit)
                                } else {
                                    de.nichu42.boxviewer.ui.theme.SensorTheme.getVisuals(s.sensorTitle).color
                                }
                                val conversion = SensorDisplayConverter.convert(
                                    rawValue = s.value,
                                    sourceUnit = s.sensorUnit,
                                    temperatureUnit = temperatureUnit,
                                    pressureUnit = pressureUnit,
                                    windUnit = windUnit,
                                    formatPressure = formatPressure
                                )
                                val displayValue = if (conversion.unit.isNullOrEmpty()) {
                                    conversion.value ?: "--"
                                } else {
                                    "${conversion.value ?: "--"} ${conversion.unit}"
                                }
                                Text(
                                    text = displayValue,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = valueColor
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = stringResource(R.string.cd_location),
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = resolvedLocation,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val measurementTime = remember(cachedSensors) {
                        viewModel.formatMeasurementTime(cachedSensors)
                    }
                    if (measurementTime.isNotBlank()) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = stringResource(R.string.cd_data_updated),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = measurementTime,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun exposureLabel(exposure: String?): String {
    return when (exposure) {
        "indoor" -> stringResource(R.string.dashboard_exposure_indoor)
        "outdoor" -> stringResource(R.string.dashboard_exposure_outdoor)
        else -> exposure ?: stringResource(R.string.dashboard_exposure_outdoor)
    }
}

@Composable
fun ReorderBoxRow(
    box: SavedBoxEntity,
    isBeingDragged: Boolean,
    modifier: Modifier,
    onUnfavoriteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .border(
                width = 1.dp,
                color = if (isBeingDragged) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isBeingDragged) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
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
                        text = box.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    box.exposure?.let {
                        Text(
                            text = exposureLabel(it),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onUnfavoriteClick,
                    modifier = Modifier.testTag("bookmark_button_reorder_${box.boxId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = stringResource(R.string.cd_remove_bookmark),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Icon(
                    imageVector = Icons.Default.Reorder,
                    contentDescription = stringResource(R.string.cd_drag_handle_reorder),
                    tint = if (isBeingDragged) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = modifier
                        .size(36.dp)
                        .padding(4.dp)
                )
            }
        }
    }
}
