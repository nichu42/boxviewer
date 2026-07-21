package de.nichu42.boxviewer.ui

import android.content.ClipData
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import de.nichu42.boxviewer.R
import de.nichu42.boxviewer.util.ApiLogger
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiLogViewerScreen(
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    val logsClearedMsg = stringResource(R.string.api_log_logs_cleared)
    val urlCopiedMsg = stringResource(R.string.api_log_url_copied)
    val responseCopiedMsg = stringResource(R.string.api_log_response_copied)
    val entryCopiedMsg = stringResource(R.string.api_log_entry_copied)

    var diagnostics by remember { mutableStateOf<Map<String, Any>?>(null) }
    var logEntries by remember { mutableStateOf<List<ApiLogger.ApiLogEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Search and Filters
    var searchQuery by remember { mutableStateOf("") }
    var selectedMethodFilter by remember { mutableStateOf("All") }
    var selectedStatusFilter by remember { mutableStateOf("All") }

    // Collapsible states
    var diagnosticsExpanded by remember { mutableStateOf(false) }
    var statsExpanded by remember { mutableStateOf(true) }

    // Bottom sheet details state
    var selectedEntry by remember { mutableStateOf<ApiLogger.ApiLogEntry?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    // Function to reload logs
    val reloadLogs = {
        isLoading = true
        scope.launch {
            val result = ApiLogger.parseLogs()
            diagnostics = result.first
            logEntries = result.second
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        reloadLogs()
    }

    // Filter logic
    val filteredEntries = remember(logEntries, searchQuery, selectedMethodFilter, selectedStatusFilter) {
        logEntries.filter { entry ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                entry.url.contains(searchQuery, ignoreCase = true) ||
                        (entry.responseJson?.contains(searchQuery, ignoreCase = true) ?: false) ||
                        (entry.error?.contains(searchQuery, ignoreCase = true) ?: false) ||
                        entry.status.toString().contains(searchQuery)
            }
            val matchesMethod = when (selectedMethodFilter) {
                "All" -> true
                "GET" -> entry.method.equals("GET", ignoreCase = true)
                "POST" -> entry.method.equals("POST", ignoreCase = true)
                else -> true
            }
            val matchesStatus = when (selectedStatusFilter) {
                "All" -> true
                "Success (2xx)" -> entry.status in 200..299
                "Client Error (4xx)" -> entry.status in 400..499
                "Server Error (5xx)" -> entry.status in 500..599
                "Failed/Exceptions" -> entry.status == 0 || entry.error != null
                else -> true
            }
            matchesSearch && matchesMethod && matchesStatus
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.api_log_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("api_log_viewer_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { reloadLogs() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = stringResource(R.string.cd_refresh_logs))
                    }
                    IconButton(
                        onClick = {
                            ApiLogger.clearLogs()
                            logEntries = emptyList()
                            diagnostics = null
                            Toast.makeText(ctx, logsClearedMsg, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("api_log_viewer_clear")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(R.string.cd_clear_logs), tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Diagnostics Panel (if diagnostics exist)
                    diagnostics?.let { diag ->
                        item {
                            DiagnosticsCollapsiblePanel(
                                diagnostics = diag,
                                isExpanded = diagnosticsExpanded,
                                onToggle = { diagnosticsExpanded = !diagnosticsExpanded }
                            )
                        }
                    }

                    // 2. Statistics Panel
                    if (logEntries.isNotEmpty()) {
                        item {
                            StatsCollapsiblePanel(
                                entries = logEntries,
                                isExpanded = statsExpanded,
                                onToggle = { statsExpanded = !statsExpanded }
                            )
                        }
                    }

                    // 3. Search and Filters Header
                    item {
                        SearchAndFiltersSection(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            selectedMethodFilter = selectedMethodFilter,
                            onMethodFilterChange = { selectedMethodFilter = it },
                            selectedStatusFilter = selectedStatusFilter,
                            onStatusFilterChange = { selectedStatusFilter = it }
                        )
                    }

                    // 4. Log Rows List
                    if (filteredEntries.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (logEntries.isEmpty()) stringResource(R.string.api_log_empty) else stringResource(R.string.api_log_no_matches),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    } else {
                        items(filteredEntries) { entry ->
                            ApiLogEntryRow(
                                entry = entry,
                                onClick = {
                                    selectedEntry = entry
                                    showBottomSheet = true
                                }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // Bottom sheet display
            if (showBottomSheet && selectedEntry != null) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    ApiLogDetailView(
                        entry = selectedEntry!!,
                        onCopyUrl = {
                            scope.launch {
                                clipboard.setClipEntry(ClipData.newPlainText("API Request URL", it).toClipEntry())
                                Toast.makeText(ctx, urlCopiedMsg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onCopyResponse = {
                            scope.launch {
                                clipboard.setClipEntry(ClipData.newPlainText("API Response", it).toClipEntry())
                                Toast.makeText(ctx, responseCopiedMsg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onCopyAll = {
                            scope.launch {
                                clipboard.setClipEntry(ClipData.newPlainText("API Log Item", it).toClipEntry())
                                Toast.makeText(ctx, entryCopiedMsg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DiagnosticsCollapsiblePanel(
    diagnostics: Map<String, Any>,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = stringResource(R.string.cd_diagnostics),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.api_log_diagnostics_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) stringResource(R.string.cd_collapse) else stringResource(R.string.cd_expand),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val timeUnknown = stringResource(R.string.time_unknown)
                    val appVer = diagnostics["appVersion"] as? String ?: timeUnknown
                    val device = diagnostics["device"] as? String ?: timeUnknown
                    val sdk = diagnostics["androidSdk"]?.toString() ?: timeUnknown
                    val date = diagnostics["date"] as? String ?: timeUnknown

                    DiagnosticsRow(label = stringResource(R.string.api_log_label_app_version), value = appVer)
                    DiagnosticsRow(label = stringResource(R.string.api_log_label_device_info), value = device)
                    DiagnosticsRow(label = stringResource(R.string.api_log_label_android_sdk), value = sdk)
                    DiagnosticsRow(label = stringResource(R.string.api_log_label_start_timestamp), value = date)
                }
            }
        }
    }
}

@Composable
fun DiagnosticsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun StatsCollapsiblePanel(
    entries: List<ApiLogger.ApiLogEntry>,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val total = entries.size
    val successes = entries.count { it.status in 200..299 }
    val clientErrors = entries.count { it.status in 400..499 }
    val serverErrors = entries.count { it.status in 500..599 }
    val exceptions = entries.count { it.status == 0 || it.error != null }
    val successRate = if (total > 0) (successes.toFloat() / total * 100).toInt() else 100
    val avgLatency = if (total > 0) entries.map { it.durationMs }.average().toInt() else 0

    // Top endpoints
    val topEndpoints = remember(entries) {
        entries.groupBy {
            try {
                val path = URL(it.url).path
                // Clean path to show generic stats
                if (path.startsWith("/boxes/")) {
                    val parts = path.split("/")
                    if (parts.size > 2 && parts[2].length >= 24) { // senseBox ID length
                        path.replace(parts[2], "{boxId}")
                    } else {
                        path
                    }
                } else {
                    path
                }
            } catch (e: Exception) {
                it.url
            }
        }.map { (path, pathEntries) ->
            Triple(path, pathEntries.size, pathEntries.map { it.durationMs }.average().toInt())
        }.sortedByDescending { it.second }.take(3)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = stringResource(R.string.cd_stats),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.api_log_stats_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) stringResource(R.string.cd_collapse) else stringResource(R.string.cd_expand),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Metrics Grid (4 items)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricCell(modifier = Modifier.weight(1f), title = stringResource(R.string.api_log_metric_requests), value = "$total")
                        MetricCell(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.api_log_metric_success_rate),
                            value = "$successRate%",
                            valueColor = when {
                                successRate >= 95 -> Color(0xFF4CAF50)
                                successRate >= 80 -> Color(0xFFFF9800)
                                else -> Color(0xFFF44336)
                            }
                        )
                        MetricCell(modifier = Modifier.weight(1f), title = stringResource(R.string.api_log_metric_avg_latency), value = "${avgLatency}ms")
                        MetricCell(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.api_log_metric_failed),
                            value = "${clientErrors + serverErrors + exceptions}",
                            valueColor = if (clientErrors + serverErrors + exceptions > 0) Color(0xFFF44336) else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Visual Distribution Bar
                    Column {
                        Text(
                            text = stringResource(R.string.api_log_status_distribution),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            if (total > 0) {
                                val totalFloat = total.toFloat()
                                val successWeight = successes.toFloat() / totalFloat
                                val clientErrorWeight = clientErrors.toFloat() / totalFloat
                                val serverErrorWeight = serverErrors.toFloat() / totalFloat
                                val exceptionWeight = exceptions.toFloat() / totalFloat

                                if (successes > 0) {
                                    Spacer(
                                        modifier = Modifier
                                            .weight(successWeight)
                                            .fillMaxHeight()
                                            .background(Color(0xFF4CAF50))
                                    )
                                }
                                if (clientErrors > 0) {
                                    Spacer(
                                        modifier = Modifier
                                            .weight(clientErrorWeight)
                                            .fillMaxHeight()
                                            .background(Color(0xFFFF9800))
                                    )
                                }
                                if (serverErrors > 0) {
                                    Spacer(
                                        modifier = Modifier
                                            .weight(serverErrorWeight)
                                            .fillMaxHeight()
                                            .background(Color(0xFFF44336))
                                    )
                                }
                                if (exceptions > 0) {
                                    Spacer(
                                        modifier = Modifier
                                            .weight(exceptionWeight)
                                            .fillMaxHeight()
                                            .background(Color(0xFF9C27B0))
                                    )
                                }
                            }
                        }
                        // Distribution Legend
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            LegendItem(color = Color(0xFF4CAF50), label = stringResource(R.string.api_log_legend_success, successes))
                            LegendItem(color = Color(0xFFFF9800), label = stringResource(R.string.api_log_legend_client_error, clientErrors))
                            LegendItem(color = Color(0xFFF44336), label = stringResource(R.string.api_log_legend_server_error, serverErrors))
                            LegendItem(color = Color(0xFF9C27B0), label = stringResource(R.string.api_log_legend_failed, exceptions))
                        }
                    }

                    // Top Endpoints Table
                    if (topEndpoints.isNotEmpty()) {
                        Column {
                            Text(
                                text = stringResource(R.string.api_log_top_endpoints),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    topEndpoints.forEachIndexed { index, (path, count, lat) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = path,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.api_log_count_format, count),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = stringResource(R.string.api_log_latency_format, lat),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        if (index < topEndpoints.size - 1) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCell(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
    }
}

@Composable
private fun statusFilterDisplayName(filter: String): String = when (filter) {
    "All" -> stringResource(R.string.filter_all)
    "Success (2xx)" -> stringResource(R.string.api_log_filter_success)
    "Client Error (4xx)" -> stringResource(R.string.api_log_filter_client_error)
    "Server Error (5xx)" -> stringResource(R.string.api_log_filter_server_error)
    "Failed/Exceptions" -> stringResource(R.string.api_log_filter_failed)
    else -> filter
}

@Composable
fun SearchAndFiltersSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedMethodFilter: String,
    onMethodFilterChange: (String) -> Unit,
    selectedStatusFilter: String,
    onStatusFilterChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text(stringResource(R.string.api_log_search_placeholder)) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = stringResource(R.string.cd_clear_search))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Method Filter Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("All", "GET", "POST").forEach { method ->
                val isSelected = selectedMethodFilter == method
                FilterChip(
                    selected = isSelected,
                    onClick = { onMethodFilterChange(method) },
                    label = { Text(if (method == "All") stringResource(R.string.filter_all) else method, modifier = Modifier.fillMaxWidth(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Status Filter Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val filters = listOf("All", "Success (2xx)", "Client Error (4xx)", "Server Error (5xx)", "Failed/Exceptions")
            var expandedFilterDropdown by remember { mutableStateOf(false) }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expandedFilterDropdown = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.api_log_status_filter_label, statusFilterDisplayName(selectedStatusFilter)),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = expandedFilterDropdown,
                    onDismissRequest = { expandedFilterDropdown = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    filters.forEach { filter ->
                        DropdownMenuItem(
                            text = { Text(statusFilterDisplayName(filter), style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                onStatusFilterChange(filter)
                                expandedFilterDropdown = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ApiLogEntryRow(
    entry: ApiLogger.ApiLogEntry,
    onClick: () -> Unit
) {
    val methodColor = when (entry.method.uppercase()) {
        "GET" -> Color(0xFF4CAF50)
        "POST" -> Color(0xFF2196F3)
        else -> Color(0xFF757575)
    }

    val statusColor = when (entry.status) {
        in 200..299 -> Color(0xFF4CAF50)
        in 400..499 -> Color(0xFFFF9800)
        in 500..599 -> Color(0xFFF44336)
        else -> Color(0xFFF44336) // 0 or network exceptions
    }

    // Clean endpoint display
    val cleanUrl = remember(entry.url) {
        try {
            val urlObj = URL(entry.url)
            val path = urlObj.path
            val query = urlObj.query
            if (query != null) "$path?$query" else path
        } catch (e: Exception) {
            entry.url
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Method and Status code badges
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.width(60.dp)
            ) {
                // Method Badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(methodColor.copy(alpha = 0.15f))
                        .padding(vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = entry.method.uppercase(),
                        color = methodColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val statusText = if (entry.status > 0) entry.status.toString() else "ERR"
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // URL Path & Time
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cleanUrl,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = entry.timestamp,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${entry.durationMs} ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (entry.error != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = entry.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ApiLogDetailView(
    entry: ApiLogger.ApiLogEntry,
    onCopyUrl: (String) -> Unit,
    onCopyResponse: (String) -> Unit,
    onCopyAll: (String) -> Unit
) {
    val ctx = LocalContext.current
    val methodColor = when (entry.method.uppercase()) {
        "GET" -> Color(0xFF4CAF50)
        "POST" -> Color(0xFF2196F3)
        else -> Color(0xFF757575)
    }

    val statusColor = when (entry.status) {
        in 200..299 -> Color(0xFF4CAF50)
        in 400..499 -> Color(0xFFFF9800)
        in 500..599 -> Color(0xFFF44336)
        else -> Color(0xFFF44336)
    }

    val prettyJson = remember(entry.responseJson) {
        formatJson(entry.responseJson)
    }

    val helpMessage = remember(entry.status, entry.error) {
        getTroubleshootingHelp(ctx, entry.status, entry.error)
    }

    val logSummaryText = remember(entry, prettyJson) {
        buildString {
            appendLine("METHOD: ${entry.method}")
            appendLine("STATUS: ${entry.status}")
            appendLine("URL: ${entry.url}")
            appendLine("TIMESTAMP: ${entry.timestamp}")
            appendLine("DURATION: ${entry.durationMs} ms")
            appendLine("APP STATE: ${entry.appState}")
            if (entry.error != null) {
                appendLine("ERROR: ${entry.error}")
            }
            if (entry.parsingResult != null) {
                appendLine("PARSING METRICS: ${entry.parsingResult}")
            }
            appendLine("RESPONSE BODY:")
            appendLine(prettyJson)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .padding(16.dp)
    ) {
        // Sheet Title
        Text(
            text = stringResource(R.string.api_log_request_details),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Content Scrollable
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // General Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(methodColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(entry.method.uppercase(), color = methodColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(statusColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (entry.status > 0) stringResource(R.string.api_log_status_format, entry.status) else stringResource(R.string.api_log_network_error),
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${entry.durationMs} ms",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Full URL selectability
                        Column {
                            Text(stringResource(R.string.api_log_label_request_url), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            SelectionContainer {
                                Text(
                                    text = entry.url,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.api_log_label_timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(entry.timestamp, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.api_log_label_app_state), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(entry.appState, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                }
            }

            // Error Message Card (if error present)
            if (entry.error != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(stringResource(R.string.api_log_label_network_exception), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            SelectionContainer {
                                Text(entry.error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }

            // On-device troubleshooting help card
            if (helpMessage != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Help,
                                    contentDescription = stringResource(R.string.cd_help),
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.api_log_troubleshooting_title),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = helpMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Parsing metrics if present
            if (entry.parsingResult != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(stringResource(R.string.api_log_label_parsing_result), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            SelectionContainer {
                                Text(entry.parsingResult, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            // Response payload pretty output
            item {
                Column {
                    Text(
                        text = stringResource(R.string.api_log_response_body_payload),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .padding(12.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                text = prettyJson,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Actions Row at the bottom
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onCopyUrl(entry.url) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.api_log_copy_url), fontSize = 11.sp)
            }
            if (entry.responseJson != null) {
                OutlinedButton(
                    onClick = { onCopyResponse(prettyJson) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.api_log_copy_json), fontSize = 11.sp)
                }
            }
            Button(
                onClick = { onCopyAll(logSummaryText) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1.2f)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.api_log_copy_entire_log), fontSize = 11.sp)
            }
        }
    }
}

fun formatJson(json: String?): String {
    if (json.isNullOrBlank()) return "No content"
    return try {
        val trimmed = json.trim()
        if (trimmed.startsWith("{")) {
            JSONObject(trimmed).toString(2)
        } else if (trimmed.startsWith("[")) {
            JSONArray(trimmed).toString(2)
        } else {
            trimmed
        }
    } catch (e: Exception) {
        json
    }
}


fun getTroubleshootingHelp(context: Context, status: Int, errorMessage: String?): String? {
    val err = errorMessage ?: ""
    return when {
        status == 404 -> context.getString(R.string.api_log_help_404)
        status == 429 -> context.getString(R.string.api_log_help_429)
        status in 500..599 -> context.getString(R.string.api_log_help_5xx, status)
        err.contains("UnknownHostException", ignoreCase = true) || err.contains("NoRouteToHostException", ignoreCase = true) -> context.getString(R.string.api_log_help_offline)
        err.contains("TimeoutException", ignoreCase = true) || err.contains("SocketTimeoutException", ignoreCase = true) -> context.getString(R.string.api_log_help_timeout)
        err.contains("ConnectException", ignoreCase = true) -> context.getString(R.string.api_log_help_refused)
        err.contains("JsonDataException", ignoreCase = true) || err.contains("JsonEncodingException", ignoreCase = true) -> context.getString(R.string.api_log_help_parsing)
        status != 0 && status !in 200..299 -> context.getString(R.string.api_log_help_http_error, status)
        errorMessage != null -> context.getString(R.string.api_log_help_unknown)
        else -> null
    }
}
