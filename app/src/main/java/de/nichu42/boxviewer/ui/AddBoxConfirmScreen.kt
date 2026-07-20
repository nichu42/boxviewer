package de.nichu42.boxviewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import de.nichu42.boxviewer.R
import de.nichu42.boxviewer.data.api.SenseBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBoxConfirmScreen(
    boxId: String,
    viewModel: SenseBoxViewModel,
    onAddToDashboard: (String) -> Unit,
    onViewDetails: (String) -> Unit,
    onCancel: () -> Unit
) {
    val boxPreview by viewModel.boxPreview.collectAsStateWithLifecycle()
    val isLoading by viewModel.isPreviewLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.previewError.collectAsStateWithLifecycle()
    val previewLocation by viewModel.previewLocation.collectAsStateWithLifecycle()
    val savedBoxes by viewModel.savedBoxes.collectAsStateWithLifecycle()

    val isAlreadySaved = remember(savedBoxes, boxId) {
        savedBoxes.any { it.boxId == boxId }
    }

    LaunchedEffect(boxId) {
        viewModel.loadBoxPreview(boxId)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearBoxPreview()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_box_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_cancel))
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.add_box_fetching),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                errorMessage != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage!!,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { viewModel.loadBoxPreview(boxId) }) {
                                Text(stringResource(R.string.action_retry))
                            }
                            Button(onClick = onCancel) {
                                Text(stringResource(R.string.action_cancel))
                            }
                        }
                    }
                }

                boxPreview != null -> {
                    BoxPreviewCard(
                        box = boxPreview!!,
                        resolvedLocation = previewLocation,
                        isAlreadySaved = isAlreadySaved
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (isAlreadySaved) {
                            Button(
                                onClick = { onViewDetails(boxId) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.action_view_details))
                            }
                            OutlinedButton(
                                onClick = onCancel,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(stringResource(R.string.action_close))
                            }
                        } else {
                            Button(
                                onClick = {
                                    viewModel.toggleFavorite(boxPreview!!)
                                    onAddToDashboard(boxId)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.action_add_to_dashboard))
                            }
                            OutlinedButton(
                                onClick = { onViewDetails(boxId) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(stringResource(R.string.action_view_details))
                            }
                            TextButton(
                                onClick = onCancel,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.action_cancel))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxPreviewCard(
    box: SenseBox,
    resolvedLocation: String?,
    isAlreadySaved: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = box.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = box.exposure ?: "outdoor",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (isAlreadySaved) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.add_box_already_saved),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!resolvedLocation.isNullOrEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = stringResource(R.string.cd_location),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = resolvedLocation,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            } else {
                box.currentLocation?.let { loc ->
                    val latStr = String.format(java.util.Locale.US, "%.4f", loc.latitude)
                    val lngStr = String.format(java.util.Locale.US, "%.4f", loc.longitude)
                    Text(
                        text = "Coords: $latStr, $lngStr",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            val sensorCount = box.sensors?.size ?: 0
            Text(
                text = "$sensorCount sensor" + if (sensorCount == 1) "" else "s",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            val sensorTitles = box.sensors?.take(3)?.map { it.title } ?: emptyList()
            if (sensorTitles.isNotEmpty()) {
                Text(
                    text = sensorTitles.joinToString(", "),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            box.description?.let { desc ->
                if (desc.trim().isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
