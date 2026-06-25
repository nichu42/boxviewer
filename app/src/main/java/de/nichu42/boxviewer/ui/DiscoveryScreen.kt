package de.nichu42.boxviewer.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
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
import kotlinx.coroutines.launch
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
                searchQuery = "Locating..."
                getCurrentGPSLocation(context, viewModel) { label ->
                    searchQuery = label
                }
            }
        }
    )



    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discover senseBoxes", fontWeight = FontWeight.Bold) },
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
                            text = "City / Location",
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
                            text = "senseBox Name / ID",
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
                                    val formattedLabel = firstAddress.getAddressLine(0) ?: fullLabel.ifBlank { "Unknown Location" }
                                    
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
                                Text("Enter City")
                            } else {
                                Text("Search by senseBox Name / ID")
                            }
                        },
                        leadingIcon = { 
                            Icon(
                                imageVector = if (isLocationSearch) Icons.Default.Place else Icons.Default.Search, 
                                contentDescription = "Search"
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
                                            contentDescription = "Run Search",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = { 
                                        searchQuery = "" 
                                        if (isLocationSearch) {
                                            viewModel.clearAutocomplete()
                                        }
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                                if (isLocationSearch) {
                                    IconButton(
                                        modifier = Modifier.testTag("gps_inline_button"),
                                        onClick = {
                                            searchQuery = "Locating..."
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
                                            contentDescription = "Use GPS Location",
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
                                            address.getAddressLine(0) ?: "Unknown Location"
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
                                contentDescription = "Filters",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Search Filters",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (filtersExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (filtersExpanded) "Collapse/Expand" else "Expand/Collapse",
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
                                    text = "Exposure",
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
                                        FilterChip(
                                            selected = selected,
                                            onClick = { viewModel.selectedExposure.value = filter },
                                            label = { Text(filter.label, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                            shape = RoundedCornerShape(8.dp),
                                            leadingIcon = if (selected) {
                                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                            } else null
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                // Last Updated Slider
                                val lastUpdatedHours by viewModel.lastUpdatedHours.collectAsStateWithLifecycle()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Last Updated Limit",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val hoursText = when {
                                        lastUpdatedHours == 1 -> "Last 1 hour"
                                        lastUpdatedHours < 24 -> "Last $lastUpdatedHours hours"
                                        lastUpdatedHours == 24 -> "Last 24 hours (1 day)"
                                        lastUpdatedHours < 168 -> {
                                            val days = lastUpdatedHours / 24
                                            val remainingHours = lastUpdatedHours % 24
                                            if (remainingHours > 0) "Last $days days $remainingHours hours" else "Last $days days"
                                        }
                                        else -> "All Time"
                                    }
                                    Text(
                                        text = hoursText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val lastUpdatedSteps = listOf(1, 3, 6, 12, 24, 169)
                                    val currentStepIndex = remember(lastUpdatedHours) {
                                        val index = lastUpdatedSteps.indexOf(lastUpdatedHours)
                                        if (index != -1) index else 0
                                    }
                                    Text("1 h", style = MaterialTheme.typography.bodySmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Slider(
                                        value = currentStepIndex.toFloat(),
                                        onValueChange = { indexFloat ->
                                            val idx = indexFloat.toInt().coerceIn(0, lastUpdatedSteps.size - 1)
                                            viewModel.setLastUpdatedHours(lastUpdatedSteps[idx])
                                        },
                                        valueRange = 0f..5f,
                                        steps = 4,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 8.dp)
                                    )
                                    Text("All Time", style = MaterialTheme.typography.bodySmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                // Search Radius Slider Row (Shown for any search with coordinates to search around)
                                val lastSearchedCoords by viewModel.lastSearchedCoords.collectAsStateWithLifecycle()
                                
                                if (lastSearchedCoords != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    val searchRadiusKm by viewModel.searchRadiusKm.collectAsStateWithLifecycle()
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Search Radius",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "$searchRadiusKm km",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("1 km", style = MaterialTheme.typography.bodySmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Slider(
                                            value = searchRadiusKm.toFloat(),
                                            onValueChange = { viewModel.setSearchRadiusKm(it.toInt()) },
                                            valueRange = 1f..1000f,
                                            onValueChangeFinished = {
                                                viewModel.updateRadiusAndQuery(searchRadiusKm, isLocationSearch)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 8.dp)
                                        )
                                        Text("1000 km", style = MaterialTheme.typography.bodySmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                contentDescription = "Search Off",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (rawDiscoveredBoxes.isNotEmpty()) "Results Filtered Out" else "No senseBoxes Found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isLocationSearch) {
                                    if (rawDiscoveredBoxes.isNotEmpty()) {
                                        "We found ${rawDiscoveredBoxes.size} senseBoxes, but they were filtered out by your current Search Filters. Try setting 'Last Updated' to 'All Time' or 'Exposure' to 'All'."
                                    } else {
                                        "Try searching for a different named area above, search by coordinates, or use the GPS button to find stations."
                                    }
                                } else {
                                    "Try checking the name spelling or verify the 24-character hexadecimal senseBox ID."
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else {
                            Icon(
                                imageVector = if (isLocationSearch) Icons.Default.Explore else Icons.Default.Sensors,
                                contentDescription = "Discover",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isLocationSearch) "Discover senseBoxes" else "Find senseBox Name / ID",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isLocationSearch) {
                                    "Search for a city, or click the GPS icon above to find weather stations near you. Bookmark your favorites to save them to your dashboard."
                                } else {
                                    "Enter a senseBox name or a 24-character senseBox ID. Remember to bookmark stations you want to keep!"
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
                                    text = "FOUND ${discoveredBoxes.size} SENSEBOXES",
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
                                            text = "Search radius: ${radiusMeters / 1000} km",
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
                                        text = "Tap the bookmark icon to save a station to your list.",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }

                    items(discoveredBoxes, key = { it.id }) { box ->
                        val isFav = isFavoritedBoxId.contains(box.id)
                        val resolvedLoc = boxLocations[box.id]
                        val lastUpdatedText = remember(box) { viewModel.formatLastUpdated(box) }
                        DiscoveredBoxCard(
                            box = box,
                            resolvedLocation = resolvedLoc,
                            lastUpdatedText = lastUpdatedText,
                            isFavorite = isFav,
                            onToggleFavorite = {
                                viewModel.toggleFavorite(box)
                                if (!isFav) {
                                    onNavigateToDashboardWithConfig(box.id)
                                }
                            },
                            onClick = { onBoxSelected(box.id) }
                        )
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
                            text = box.exposure ?: "outdoor",
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                // Show Geocoded Location
                if (!resolvedLocation.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Location",
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
                            maxLines = 1
                        )
                    }
                } else {
                    box.currentLocation?.let { loc ->
                        val latStr = String.format(Locale.US, "%.4f", loc.latitude)
                        val lngStr = String.format(Locale.US, "%.4f", loc.longitude)
                        Text(
                            text = "Coords: $latStr, $lngStr",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }


                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Updated time",
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
                        text = "Sensors: " + sensorTitles.joinToString(", "),
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
                    contentDescription = if (isFavorite) "Remove Bookmark" else "Save Bookmark",
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
            handleLocationFailure(viewModel, onResult)
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
                            handleLocationFailure(viewModel, onResult)
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
                                handleLocationFailure(viewModel, onResult)
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                    }, 5000)
                }
            } else {
                handleLocationFailure(viewModel, onResult)
            }
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        handleLocationFailure(viewModel, onResult)
    }
}

private fun handleLocationFailure(
    viewModel: SenseBoxViewModel,
    onResult: (String) -> Unit
) {
    onResult("")
    viewModel.setErrorMessage("Unable to determine current location. Please search manually using the search bar.")
}
