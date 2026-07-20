package de.nichu42.boxviewer.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.focusProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import de.nichu42.boxviewer.R
import de.nichu42.boxviewer.data.api.SenseBox
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(
    viewModel: SenseBoxViewModel,
    onBoxSelected: (String) -> Unit,
    onNavigateToDashboardWithConfig: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val discoveredBoxes by viewModel.discoveredBoxes.collectAsStateWithLifecycle()
    val savedBoxes by viewModel.savedBoxes.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val selectedExposure by viewModel.selectedExposure.collectAsStateWithLifecycle()
    val boxLocations by viewModel.boxLocations.collectAsStateWithLifecycle()
    val rawDiscoveredBoxes by viewModel.rawDiscoveredBoxes.collectAsStateWithLifecycle()
    val hasSearchBeenDone by viewModel.hasSearchBeenDone.collectAsStateWithLifecycle()
    val searchRadiusUsed by viewModel.searchRadiusUsed.collectAsStateWithLifecycle()
    val isLocationSearch by viewModel.isLocationSearch.collectAsStateWithLifecycle()
    val lastUpdatedMinutes by viewModel.lastUpdatedMinutes.collectAsStateWithLifecycle()
    val lastSearchedCoords by viewModel.lastSearchedCoords.collectAsStateWithLifecycle()

    var visibleLimit by rememberSaveable(discoveredBoxes) { mutableIntStateOf(5) }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    
    var filtersExpanded by remember { mutableStateOf(false) }
    val isFavoritedBoxId = remember(savedBoxes) {
        savedBoxes.map { it.boxId }.toSet()
    }

    // Permission launcher for GPS
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                searchQuery = context.getString(R.string.discovery_locating)
                getCurrentGPSLocation(context, viewModel) { label ->
                    searchQuery = label
                }
            }
        }
    )

    // Launcher for external barcode scanner apps (ZXing-compatible intent protocol)
    val barcodeScanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val raw = result.data?.getStringExtra("SCAN_RESULT") ?: return@rememberLauncherForActivityResult
            // Extract a 24-char hex box ID from an openSenseMap URL or use the raw value directly
            val boxId = Regex("[0-9a-fA-F]{24}").find(raw)?.value ?: raw.trim()
            searchQuery = boxId
            viewModel.searchBoxes(boxId)
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.discovery_title), fontWeight = FontWeight.Bold) },
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
            
            // Search Mode Selectors (asymmetric, modern, distinct pill buttons)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val colorSelected = MaterialTheme.colorScheme.primaryContainer
                val colorUnselected = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                val textColorSelected = MaterialTheme.colorScheme.onPrimaryContainer
                val textColorUnselected = MaterialTheme.colorScheme.onSurfaceVariant

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isLocationSearch) colorSelected else colorUnselected)
                        .clickable(
                            enabled = true,
                            onClick = { 
                                searchQuery = ""
                                viewModel.setIsLocationSearch(true) 
                            }
                        )
                        .focusProperties { canFocus = false },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isLocationSearch) textColorSelected else textColorUnselected
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.discovery_search_mode_location),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLocationSearch) textColorSelected else textColorUnselected
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (!isLocationSearch) colorSelected else colorUnselected)
                        .clickable(
                            enabled = true,
                            onClick = { 
                                searchQuery = ""
                                viewModel.setIsLocationSearch(false) 
                            }
                        )
                        .focusProperties { canFocus = false },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (!isLocationSearch) textColorSelected else textColorUnselected
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.discovery_search_mode_box_id),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isLocationSearch) textColorSelected else textColorUnselected
                        )
                    }
                }
            }

            // Search Input Header with In-Line GPS Option and Autocomplete Suggestions
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Column {
                    val autocompleteResults by viewModel.autocompleteResults.collectAsStateWithLifecycle()
                    
                    val performSearch = {
                        try {
                            val results = viewModel.autocompleteResults.value
                            if (isLocationSearch) {
                                if (results.isNotEmpty()) {
                                    val firstAddress = results.first()
                                    val city = firstAddress.locality ?: firstAddress.subAdminArea ?: firstAddress.subLocality
                                    val state = firstAddress.adminArea
                                    val country = firstAddress.countryName
                                    val fullLabel = listOfNotNull(city, state, country)
                                        .filter { it.isNotBlank() }
                                        .joinToString(", ")
                                    val formattedLabel = firstAddress.getAddressLine(0) ?: fullLabel.ifBlank { context.getString(R.string.discovery_unknown_location) }
                                    
                                    searchQuery = formattedLabel
                                    viewModel.searchByAddress(firstAddress)
                                } else {
                                    viewModel.searchByLocation(searchQuery)
                                }
                            } else {
                                viewModel.searchBoxes(searchQuery)
                            }
                            viewModel.clearAutocomplete()
                            coroutineScope.launch {
                                focusManager.clearFocus()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { 
                            searchQuery = it 
                            if (isLocationSearch) {
                                viewModel.onLocationQueryChanged(it)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input_field")
                            .onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter) {
                                    if (keyEvent.type == KeyEventType.KeyUp) {
                                        performSearch()
                                    }
                                    true
                                } else {
                                    false
                                }
                            },
                        placeholder = { 
                            if (isLocationSearch) {
                                Text(stringResource(R.string.discovery_placeholder_city))
                            } else {
                                Text(stringResource(R.string.discovery_placeholder_box_id))
                            }
                        },
                        leadingIcon = { 
                            Icon(
                                imageVector = if (isLocationSearch) Icons.Default.Place else Icons.Default.Search, 
                                contentDescription = stringResource(R.string.cd_search)
                            ) 
                        },
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        modifier = Modifier.testTag("search_exec_button"),
                                        onClick = { performSearch() }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = stringResource(R.string.cd_run_search),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = { 
                                        searchQuery = "" 
                                        if (isLocationSearch) {
                                            viewModel.clearAutocomplete()
                                        }
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.cd_clear))
                                    }
                                }
                                if (isLocationSearch) {
                                    IconButton(
                                        modifier = Modifier.testTag("gps_inline_button"),
                                        onClick = {
                                            searchQuery = context.getString(R.string.discovery_locating)
                                            viewModel.clearAutocomplete()
                                            val check = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                                            if (check == PackageManager.PERMISSION_GRANTED) {
                                                getCurrentGPSLocation(context, viewModel) { label ->
                                                    searchQuery = label
                                                }
                                            } else {
                                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MyLocation,
                                            contentDescription = stringResource(R.string.cd_use_gps),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else {
                                    IconButton(
                                        modifier = Modifier.testTag("barcode_scan_button"),
                                        onClick = {
                                            val intent = Intent("com.google.zxing.client.android.SCAN").apply {
                                                putExtra("SCAN_MODE", "QR_CODE_MODE")
                                            }
                                            try {
                                                barcodeScanLauncher.launch(intent)
                                            } catch (_: ActivityNotFoundException) {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.discovery_no_barcode_scanner),
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = stringResource(R.string.cd_scan_qr),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { performSearch() }),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Autocomplete Suggestions popup panel
                    AnimatedVisibility(visible = isLocationSearch && autocompleteResults.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                val displayAddresses = autocompleteResults
                                    .map { address ->
                                        val city = address.locality ?: address.subAdminArea ?: address.subLocality
                                        val state = address.adminArea
                                        val country = address.countryName
                                        val fullLabel = listOfNotNull(city, state, country)
                                            .filter { it.isNotBlank() }
                                            .joinToString(", ")
                                        val formattedLabel = fullLabel.ifBlank {
                                            address.getAddressLine(0) ?: context.getString(R.string.discovery_unknown_location)
                                        }
                                        val finalLabel = address.getAddressLine(0) ?: formattedLabel
                                        Pair(address, finalLabel)
                                    }
                                    .distinctBy { it.second }
                                    .take(3)

                                displayAddresses.forEach { (address, formattedLabel) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                searchQuery = formattedLabel
                                                viewModel.searchByAddress(address)
                                                focusManager.clearFocus()
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = formattedLabel,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Filters Section Card: Collapsible and contains Search Radius Slider (shown only for Location search let results exist)
            AnimatedVisibility(visible = isLocationSearch && rawDiscoveredBoxes.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        // Clickable header/bar to expand/collapse
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { filtersExpanded = !filtersExpanded }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = stringResource(R.string.cd_filters),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.discovery_search_filters),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (filtersExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (filtersExpanded) stringResource(R.string.cd_collapse) else stringResource(R.string.cd_expand),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        AnimatedVisibility(visible = filtersExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(bottom = 10.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    thickness = 1.dp
                                )

                                // Exposure Type Selector
                                Text(
                                    text = stringResource(R.string.discovery_exposure_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SenseBoxViewModel.ExposureFilter.entries.forEach { filter ->
                                        val selected = (selectedExposure == filter)
                                        val filterLabel = stringResource(when (filter) {
                                            SenseBoxViewModel.ExposureFilter.OUTDOOR -> R.string.exposure_outdoor
                                            SenseBoxViewModel.ExposureFilter.INDOOR -> R.string.exposure_indoor
                                            SenseBoxViewModel.ExposureFilter.ALL -> R.string.exposure_all
                                        })
                                        FilterChip(
                                            selected = selected,
                                            onClick = { viewModel.selectedExposure.value = filter },
                                            label = { Text(filterLabel, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                            shape = RoundedCornerShape(8.dp),
                                            leadingIcon = if (selected) {
                                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                            } else null
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                // Last Updated Slider
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.discovery_last_updated_limit),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val minutesText = when {
                                        lastUpdatedMinutes == 15 -> stringResource(R.string.discovery_last_updated_15m)
                                        lastUpdatedMinutes == 30 -> stringResource(R.string.discovery_last_updated_30m)
                                        lastUpdatedMinutes == 60 -> stringResource(R.string.discovery_last_updated_1h)
                                        lastUpdatedMinutes < 1440 -> {
                                            val hours = lastUpdatedMinutes / 60
                                            stringResource(R.string.discovery_last_updated_hours, hours)
                                        }
                                        lastUpdatedMinutes == 1440 -> stringResource(R.string.discovery_last_updated_24h)
                                        else -> stringResource(R.string.discovery_last_updated_all_time)
                                    }
                                    Text(
                                        text = minutesText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val lastUpdatedSteps = listOf(15, 30, 60, 180, 360, 720, 1440, 999999)
                                    val currentStepIndex = remember(lastUpdatedMinutes) {
                                        val index = lastUpdatedSteps.indexOf(lastUpdatedMinutes)
                                        if (index != -1) index else 2
                                    }
                                    Text(stringResource(R.string.discovery_15m_short), style = MaterialTheme.typography.bodySmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Slider(
                                        value = currentStepIndex.toFloat(),
                                        onValueChange = { indexFloat ->
                                            val idx = indexFloat.toInt().coerceIn(0, lastUpdatedSteps.lastIndex)
                                            viewModel.setLastUpdatedMinutes(lastUpdatedSteps[idx])
                                        },
                                        valueRange = 0f..lastUpdatedSteps.lastIndex.toFloat(),
                                        steps = lastUpdatedSteps.lastIndex - 1,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 8.dp)
                                    )
                                    Text(stringResource(R.string.discovery_last_updated_all_time), style = MaterialTheme.typography.bodySmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                // Search Radius Slider Row (Shown for any search with coordinates to search around)
                                if (lastSearchedCoords != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    val searchRadiusKm by viewModel.searchRadiusKm.collectAsStateWithLifecycle()
                                     
                                    val radiusSteps = listOf(2, 5, 10, 25, 50, 75, 100, 250)
                                    val currentRadiusIdx = remember(searchRadiusKm) {
                                         radiusSteps.mapIndexed { index, value -> index to kotlin.math.abs(value - searchRadiusKm) }
                                             .minByOrNull { it.second }?.first ?: 0
                                    }
                                    var sliderIndex by remember(currentRadiusIdx) {
                                         mutableFloatStateOf(currentRadiusIdx.toFloat())
                                    }
                                    val displayRadius = remember(sliderIndex) {
                                         radiusSteps[(sliderIndex + 0.5f).toInt().coerceIn(0, radiusSteps.lastIndex)]
                                    }

                                    Row(
                                         modifier = Modifier.fillMaxWidth(),
                                         horizontalArrangement = Arrangement.SpaceBetween,
                                         verticalAlignment = Alignment.CenterVertically
                                     ) {
                                         Text(
                                             text = stringResource(R.string.discovery_radius_label),
                                             style = MaterialTheme.typography.labelSmall,
                                             fontWeight = FontWeight.Bold,
                                             color = MaterialTheme.colorScheme.onSurfaceVariant
                                         )
                                         Text(
                                             text = stringResource(R.string.discovery_radius_format, displayRadius),
                                             style = MaterialTheme.typography.labelSmall,
                                             fontWeight = FontWeight.Bold,
                                             color = MaterialTheme.colorScheme.primary
                                         )
                                     }

                                     Row(
                                         verticalAlignment = Alignment.CenterVertically,
                                         modifier = Modifier.fillMaxWidth()
                                     ) {
                                         Text(stringResource(R.string.discovery_2km_short), style = MaterialTheme.typography.bodySmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                         Slider(
                                             value = sliderIndex,
                                             onValueChange = { sliderIndex = it },
                                             valueRange = 0f..(radiusSteps.size - 1).toFloat(),
                                             steps = radiusSteps.size - 2,
                                             onValueChangeFinished = {
                                                 val chosenRadius = radiusSteps[(sliderIndex + 0.5f).toInt().coerceIn(0, radiusSteps.lastIndex)]
                                                 viewModel.setSearchRadiusKm(chosenRadius)
                                                 viewModel.updateRadiusAndQuery(chosenRadius, isLocationSearch)
                                             },
                                             modifier = Modifier
                                                 .weight(1f)
                                                 .padding(horizontal = 8.dp)
                                         )
                                         Text(stringResource(R.string.discovery_250km_short), style = MaterialTheme.typography.bodySmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                     }
                                }
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            }

            AnimatedVisibility(visible = errorMessage != null) {
                errorMessage?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Results List
            if (discoveredBoxes.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (hasSearchBeenDone) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = stringResource(R.string.cd_search_off),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (rawDiscoveredBoxes.isNotEmpty()) stringResource(R.string.discovery_results_filtered_out) else stringResource(R.string.discovery_no_senseboxes_found),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isLocationSearch) {
                                    if (rawDiscoveredBoxes.isNotEmpty()) {
                                        stringResource(R.string.discovery_results_filtered_message, rawDiscoveredBoxes.size)
                                    } else {
                                        stringResource(R.string.discovery_no_results_location_hint)
                                    }
                                } else {
                                    stringResource(R.string.discovery_no_results_box_hint)
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else {
                            Icon(
                                imageVector = if (isLocationSearch) Icons.Default.Explore else Icons.Default.Sensors,
                                contentDescription = stringResource(R.string.cd_discover),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isLocationSearch) stringResource(R.string.discovery_button_discover) else stringResource(R.string.discovery_button_find_box),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isLocationSearch) {
                                    stringResource(R.string.discovery_empty_location_hint)
                                } else {
                                    stringResource(R.string.discovery_empty_box_hint)
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                val visibleBoxes = remember(discoveredBoxes, visibleLimit) {
                    discoveredBoxes.take(visibleLimit)
                }
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        val radiusMeters = searchRadiusUsed
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.discovery_found_count, discoveredBoxes.size),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (radiusMeters != null) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.discovery_search_radius_format, radiusMeters / 1000),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.discovery_save_hint),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }

                    items(visibleBoxes, key = { it.id }) { box ->
                        val isFav = isFavoritedBoxId.contains(box.id)
                        val resolvedLoc = boxLocations[box.id]
                        val lastUpdatedText = remember(box) { viewModel.formatLastUpdated(box) }
                        val hasOutdated = remember(box, lastUpdatedMinutes) {
                            viewModel.hasOutdatedSensors(box, lastUpdatedMinutes)
                        }
                        val distanceKm = remember(box, lastSearchedCoords) {
                            val ref = lastSearchedCoords
                            val boxCoords = box.currentLocation?.coordinates
                            if (ref != null && boxCoords != null && boxCoords.size >= 2) {
                                viewModel.calculateDistanceKm(ref.second, ref.first, boxCoords[1], boxCoords[0])
                            } else {
                                null
                            }
                        }
                        DiscoveredBoxCard(
                            box = box,
                            resolvedLocation = resolvedLoc,
                            lastUpdatedText = lastUpdatedText,
                            isFavorite = isFav,
                            hasOutdatedSensors = hasOutdated,
                            distanceKm = distanceKm,
                            onToggleFavorite = {
                                viewModel.toggleFavorite(box)
                                if (!isFav) {
                                    onNavigateToDashboardWithConfig(box.id)
                                }
                            },
                            onClick = { onBoxSelected(box.id) }
                        )
                    }

                    if (discoveredBoxes.size > visibleLimit) {
                        item {
                            val remaining = discoveredBoxes.size - visibleLimit
                            OutlinedButton(
                                onClick = { visibleLimit += 5 },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = stringResource(R.string.cd_show_more),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.discovery_show_more_format, remaining),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoveredBoxCard(
    box: SenseBox,
    resolvedLocation: String?,
    lastUpdatedText: String,
    isFavorite: Boolean,
    hasOutdatedSensors: Boolean,
    distanceKm: Double? = null,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("discovered_box_card_${box.id}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = box.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = when (box.exposure?.lowercase(Locale.getDefault())) {
                                "indoor" -> stringResource(R.string.discovery_exposure_indoor)
                                "outdoor" -> stringResource(R.string.discovery_exposure_outdoor)
                                else -> stringResource(R.string.discovery_exposure_outdoor)
                            },
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                // Show Geocoded Location & Distance
                if (!resolvedLocation.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = stringResource(R.string.cd_location),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = resolvedLocation,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (distanceKm != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.discovery_distance_format, distanceKm),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                } else {
                    box.currentLocation?.let { loc ->
                        val latStr = String.format(Locale.getDefault(), "%.4f", loc.latitude)
                        val lngStr = String.format(Locale.getDefault(), "%.4f", loc.longitude)
                        Row(
                            modifier = Modifier.padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.discovery_coords_format, latStr, lngStr),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            if (distanceKm != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.discovery_distance_format, distanceKm),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }


                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = stringResource(R.string.cd_updated_time),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = lastUpdatedText,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (hasOutdatedSensors) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = stringResource(R.string.discovery_some_metrics_outdated),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.discovery_some_metrics_outdated),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                box.description?.let { desc ->
                    if (desc.trim().isNotEmpty()) {
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Sensors horizontal preview tag
                val sensorTitles = box.sensors?.take(3)?.map { it.title } ?: emptyList()
                if (sensorTitles.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.discovery_sensors_format, sensorTitles.joinToString(", ")),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Bookmark Action Button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.testTag("bookmark_button_${box.id}")
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = if (isFavorite) stringResource(R.string.cd_remove_bookmark) else stringResource(R.string.cd_save_bookmark),
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun getCurrentGPSLocation(
    context: Context,
    viewModel: SenseBoxViewModel,
    onResult: (String) -> Unit
) {
    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
        if (locationManager == null) {
            handleLocationFailure(context, viewModel, onResult)
            return
        }

        // Try GPS provider last known location
        val gpsLoc = try {
            if (locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
            } else null
        } catch (e: SecurityException) {
            null
        }

        // Try Network provider last known location
        val netLoc = try {
            if (locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
            } else null
        } catch (e: SecurityException) {
            null
        }

        // Try Passive provider last known location
        val passiveLoc = try {
            locationManager.getLastKnownLocation(android.location.LocationManager.PASSIVE_PROVIDER)
        } catch (e: SecurityException) {
            null
        }

        // Select the best/most recent/highest accuracy location
        val bestLocation = listOfNotNull(gpsLoc, netLoc, passiveLoc)
            .maxByOrNull { it.time }

        if (bestLocation != null) {
            viewModel.findBoxesNear(bestLocation.longitude, bestLocation.latitude)
            viewModel.getAddressFromLocation(bestLocation.latitude, bestLocation.longitude) { label ->
                onResult(label)
            }
        } else {
            // No last known location found, try starting a single update from the available provider
            val provider = when {
                locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) -> android.location.LocationManager.GPS_PROVIDER
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) -> android.location.LocationManager.NETWORK_PROVIDER
                else -> null
            }

            if (provider != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    locationManager.getCurrentLocation(
                        provider,
                        null,
                        context.mainExecutor
                    ) { loc ->
                        if (loc != null) {
                            viewModel.findBoxesNear(loc.longitude, loc.latitude)
                            viewModel.getAddressFromLocation(loc.latitude, loc.longitude) { label ->
                                onResult(label)
                            }
                        } else {
                            handleLocationFailure(context, viewModel, onResult)
                        }
                    }
                } else {
                    var triggered = false
                    val listener = object : android.location.LocationListener {
                        override fun onLocationChanged(loc: android.location.Location) {
                            if (!triggered) {
                                triggered = true
                                locationManager.removeUpdates(this)
                                viewModel.findBoxesNear(loc.longitude, loc.latitude)
                                viewModel.getAddressFromLocation(loc.latitude, loc.longitude) { label ->
                                    onResult(label)
                                }
                            }
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onStatusChanged(p: String?, s: Int, e: android.os.Bundle?) {}
                        override fun onProviderEnabled(p: String) {}
                        override fun onProviderDisabled(p: String) {}
                    }
                    locationManager.requestLocationUpdates(provider, 0L, 0f, listener, context.mainLooper)
                    // Safety timeout
                    android.os.Handler(context.mainLooper).postDelayed({
                        try {
                            if (!triggered) {
                                triggered = true
                                locationManager.removeUpdates(listener)
                                    handleLocationFailure(context, viewModel, onResult)
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                    }, 5000)
                }
            } else {
                handleLocationFailure(context, viewModel, onResult)
            }
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        handleLocationFailure(context, viewModel, onResult)
    }
}

private fun handleLocationFailure(
    context: Context,
    viewModel: SenseBoxViewModel,
    onResult: (String) -> Unit
) {
    onResult("")
    viewModel.setErrorMessage(context.getString(R.string.discovery_error_location))
}
