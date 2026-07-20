package de.nichu42.boxviewer.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.io.BufferedReader
import java.io.InputStreamReader
import de.nichu42.boxviewer.R
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.annotation.StringRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    viewModel: SenseBoxViewModel,
    onViewLicense: () -> Unit = {},
    onViewThirdPartyLicenses: () -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val globalStats by viewModel.globalStats.collectAsStateWithLifecycle()
    val isLoadingStats by viewModel.isLoadingStats.collectAsStateWithLifecycle()
    val statsError by viewModel.statsError.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.fetchGlobalStats()
    }

    val versionName = remember(context) {
        try {
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "0.10"
        } catch (_: Exception) {
            "0.10"
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {


            // App Branding Block - Modern, High-End Compose Vector Branding (Fully Crash-Proof)
            Image(
                painter = painterResource(id = R.drawable.boxviewer_white_bg),
                contentDescription = stringResource(R.string.cd_app_logo),
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Fit
            )

            Text(
                text = stringResource(R.string.about_version_label, versionName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 1. App Homepage & Developer Support Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("about_app_links_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.about_section_home_support),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.about_home_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // GitHub Link Button
                    Button(
                        onClick = {
                            try {
                                uriHandler.openUri("https://github.com/nichu42/boxviewer")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("link_homepage")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = stringResource(R.string.cd_github_project),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.about_link_github),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.about_support_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.about_support_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Ko-fi Button
                        Button(
                            onClick = {
                                try {
                                    uriHandler.openUri("https://ko-fi.com/nichu42")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("link_kofi")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = stringResource(R.string.cd_kofi),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.about_link_kofi),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Liberapay Button
                        Button(
                            onClick = {
                                try {
                                    uriHandler.openUri("https://liberapay.com/nichu42")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("link_liberapay")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = stringResource(R.string.cd_liberapay),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.about_link_liberapay),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 2. Copyright and License Information Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("about_info_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.about_copyright),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.about_gpl_summary_1),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.about_gpl_summary_2),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // GNU GPL v3 Logo from the uploaded PNG resource
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.gplv3_with_text_136x68),
                            contentDescription = stringResource(R.string.cd_gpl_logo),
                            modifier = Modifier
                                .height(64.dp)
                                .fillMaxWidth(0.6f),
                            contentScale = ContentScale.Fit
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onViewLicense,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("view_license_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = stringResource(R.string.cd_license),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.about_view_license),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onViewThirdPartyLicenses,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("view_third_party_licenses_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.cd_third_party_licenses),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.about_third_party_licenses),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 3. Data & Attribution Card (openSenseMap platform info)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("about_data_attribution_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.about_section_data_attribution),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.about_data_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Network Stats Row inside the card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.about_stats_header),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (isLoadingStats) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 1.5.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else if (statsError != null) {
                                    Text(
                                        stringResource(R.string.about_stats_offline),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (statsError != null) {
                                Text(
                                    text = stringResource(R.string.about_stats_failed),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else if (globalStats != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    // Stat 1: senseBoxes
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = globalStats?.boxesCount ?: "-",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(R.string.about_label_stations),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    // Stat 2: measurements
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = globalStats?.measurementsCount ?: "-",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(R.string.about_label_measurements),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    // Stat 3: last minute
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.about_active_rate_value, globalStats?.measurementsLastMinute ?: "-"),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = stringResource(R.string.about_label_active_rate),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                // Placeholders
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    listOf("...", "...", "...").forEach { placeholder ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = placeholder,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.about_what_is_opensensemap),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.about_opensensemap_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.about_who_operates),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.about_operator_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.about_support_opensensemap_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.about_support_opensensemap_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Support Links Section - interactive buttons for awesome usability under 48.dp
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Explore openSenseMap Link
                        Button(
                            onClick = {
                                try {
                                    uriHandler.openUri("https://opensensemap.org")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("link_opensensemap")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = stringResource(R.string.cd_website),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.about_link_opensensemap),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        // Build senseBox Link
                        Button(
                            onClick = {
                                try {
                                    uriHandler.openUri("https://sensebox.de")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("link_sensebox")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = stringResource(R.string.cd_build),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.about_link_sensebox),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        // Support Mission / Donate Link
                        Button(
                            onClick = {
                                try {
                                    uriHandler.openUri("https://www.betterplace.org/en/projects/89947-opensensemap-org-the-free-map-for-environmental-data")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("link_donate")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = stringResource(R.string.cd_donate),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.about_link_betterplace),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 4. Affiliation Disclaimer Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("about_disclaimer_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = stringResource(R.string.cd_attention_warning),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.about_disclaimer_title),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.about_disclaimer_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var licenseText by remember { mutableStateOf(context.getString(R.string.about_loading_license)) }
    LaunchedEffect(context, LocalConfiguration.current) {
        licenseText = loadLicenseText(context)
    }
    
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_license_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("license_back_button")
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
                .statusBarsPadding()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(scrollState)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = licenseText,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                VerticalScrollbar(
                    scrollState = scrollState,
                    modifier = Modifier
                        .width(14.dp)
                        .fillMaxHeight()
                )
            }
        }
    }
}

private fun loadLicenseText(context: android.content.Context): String {
    return try {
        val inputStream = context.resources.openRawResource(R.raw.gpl_license)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val text = reader.readText()
        reader.close()
        normalizeLicenseText(text)
    } catch (_: Exception) {
        context.getString(R.string.about_license_load_error)
    }
}

/**
 * Parses and formats raw monospace license text.
 * It collapses lines within a paragraph but keeps double-newlines and indents of headers / list bullets.
 */
fun normalizeLicenseText(rawText: String): String {
    val cleanText = rawText.replace("\r\n", "\n").replace("\r", "\n")
    val paragraphs = cleanText.split(Regex("\\n\\s*\\n"))
    
    return paragraphs.mapNotNull { paragraph ->
        val lines = paragraph.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return@mapNotNull null
        
        // Check if paragraph matches title, subtitle, or standard document metadata headers
        val isTitleOrHeader = lines.any {
            it.equals("GNU GENERAL PUBLIC LICENSE", ignoreCase = true) || 
            it.startsWith("Version", ignoreCase = true) || 
            it.equals("Preamble", ignoreCase = true) || 
            it.equals("TERMS AND CONDITIONS", ignoreCase = true)
        }
        
        if (isTitleOrHeader || (lines.size <= 2 && lines.all { it.length < 50 })) {
            // Keep them as separate lines but fully left-aligned/trimmed
            lines.joinToString("\n")
        } else {
            // Check if first line states Copyright. If so, split it separately from the flowing text
            val firstLine = lines.first()
            if (firstLine.startsWith("Copyright (C)", ignoreCase = true) && lines.size > 1) {
                val restOfLines = lines.drop(1)
                val joinedRest = restOfLines.joinToString(" ")
                "$firstLine\n$joinedRest"
            } else {
                // Standard block paragraph or a long list item description.
                // Join them into a single continuous stream of characters so that Jetpack Compose
                // wraps them fluidly with no jagged lines or ragged margins.
                lines.joinToString(" ")
            }
        }
    }.joinToString("\n\n")
}

/**
 * A highly responsive, draggable vertical scrollbar that enables quick navigation
 * on long scrollable contents in Jetpack Compose, supporting drag gestures and track taps.
 */
@Composable
fun VerticalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    if (scrollState.maxValue <= 0) return

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .background(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(2.dp)
    ) {
        val maxHeightPx = constraints.maxHeight.toFloat()
        val thumbHeightDp = 48.dp
        val density = LocalDensity.current
        val thumbHeightPx = with(density) { thumbHeightDp.toPx() }
        
        val trackHeightPx = maxHeightPx - thumbHeightPx
        
        // Track tap gesture to instantly snap scroll location
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(trackHeightPx, scrollState.maxValue) {
                    detectTapGestures { offset ->
                        if (trackHeightPx > 0) {
                            val clickFraction = (offset.y - thumbHeightPx / 2f) / trackHeightPx
                            val targetFraction = clickFraction.coerceIn(0f, 1f)
                            val targetScroll = (targetFraction * scrollState.maxValue).toInt()
                            scrollState.dispatchRawDelta(calcScrollDelta(scrollState, targetScroll))
                        }
                    }
                }
        ) {
            // Interactive Dragable Scrollbar Thumb
            Box(
                modifier = Modifier
                    .offset { calcThumbOffset(scrollState, trackHeightPx) }
                    .fillMaxWidth()
                    .height(thumbHeightDp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .pointerInput(trackHeightPx, scrollState.maxValue) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            if (trackHeightPx > 0) {
                                val ratio = scrollState.maxValue.toFloat() / trackHeightPx
                                val delta = dragAmount.y * ratio
                                scrollState.dispatchRawDelta(delta)
                            }
                        }
                    }
            )
        }
    }
}

private fun calcThumbOffset(scrollState: ScrollState, trackHeightPx: Float): IntOffset {
    val fraction = if (scrollState.maxValue > 0) {
        scrollState.value.toFloat() / scrollState.maxValue.toFloat()
    } else {
        0f
    }
    return IntOffset(0, (fraction * trackHeightPx).roundToInt())
}

private fun calcScrollDelta(scrollState: ScrollState, targetScroll: Int): Float {
    return targetScroll.toFloat() - scrollState.value.toFloat()
}

// Custom Third Party Licenses Screen (Option B) - 100% stable, custom styled, robust
enum class LibraryCategory(@StringRes val displayNameRes: Int) {
    CORE_UI(R.string.library_category_core_ui),
    NETWORKING(R.string.library_category_networking),
    DATABASE_CONCURRENCY(R.string.library_category_database_concurrency),
    UTILITIES(R.string.library_category_utilities),
    DATA_SERVICES(R.string.library_category_data_services),
    TESTING(R.string.library_category_testing)
}

data class ThirdPartyLib(
    val name: String,
    val author: String,
    val description: String,
    val licenseName: String,
    val licenseText: String,
    val url: String,
    val category: LibraryCategory
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThirdPartyLicensesScreen(
    onBack: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val scrollState = rememberScrollState()

    val apache2 = """
        Licensed under the Apache License, Version 2.0 (the "License");
        you may not use this file except in compliance with the License.
        You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

        Unless required by applicable law or agreed to in writing, software
        distributed under the License is distributed on an "AS IS" BASIS,
        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        See the License for the specific language governing permissions and
        limitations under the License.
    """.trimIndent()

    val mit = """
        Permission is hereby granted, free of charge, to any person obtaining a copy
        of this software and associated documentation files (the "Software"), to deal
        in the Software without restriction, including without limitation the rights
        to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
        copies of the Software, and to permit persons to whom the Software is
        furnished to do so, subject to the following conditions:

        The above copyright notice and this permission notice shall be included in all
        copies or substantial portions of the Software.

        THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
        IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
        FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
        AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
        LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
        OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
        SOFTWARE.
    """.trimIndent()

    val libraries = remember {
        listOf(
            ThirdPartyLib(
                name = "Jetpack Compose & AndroidX",
                author = "Google LLC",
                description = "Modern and declarative UI toolkit, components, life cycle systems, and core tools used to build high-end Android products.",
                licenseName = "Apache License 2.0",
                licenseText = apache2,
                url = "https://developer.android.com/jetpack/compose",
                category = LibraryCategory.CORE_UI
            ),
            ThirdPartyLib(
                name = "Retrofit",
                author = "Square, Inc.",
                description = "A type-safe HTTP client for Android and JVM, used to translate openSenseMap REST APIs into clean Kotlin interfaces.",
                licenseName = "Apache License 2.0",
                licenseText = apache2,
                url = "https://github.com/square/retrofit",
                category = LibraryCategory.NETWORKING
            ),
            ThirdPartyLib(
                name = "OkHttp Engine & Interceptor",
                author = "Square, Inc.",
                description = "Efficient modern connection multiplexing HTTP logging HTTP client infrastructure.",
                licenseName = "Apache License 2.0",
                licenseText = apache2,
                url = "https://github.com/square/okhttp",
                category = LibraryCategory.NETWORKING
            ),
            ThirdPartyLib(
                name = "Moshi Core & Codegen",
                author = "Square, Inc.",
                description = "Modern JSON library for Android, Kotlin, and Java, facilitating rapid type-safe JSON serialization/deserialization.",
                licenseName = "Apache License 2.0",
                licenseText = apache2,
                url = "https://github.com/square/moshi",
                category = LibraryCategory.NETWORKING
            ),
            ThirdPartyLib(
                name = "Kotlinx Coroutines & Flow",
                author = "JetBrains s.r.o.",
                description = "Library support for Kotlin coroutines, facilitating clean asynchronous reactive data programming and state flow bindings.",
                licenseName = "Apache License 2.0",
                licenseText = apache2,
                url = "https://github.com/Kotlin/kotlinx.coroutines",
                category = LibraryCategory.DATABASE_CONCURRENCY
            ),
            ThirdPartyLib(
                name = "Room Persistence Database",
                author = "Google LLC",
                description = "Abstraction layer over SQLite, enabling robust, offline-capable structured query caching of user-added environmental sensor boxes.",
                licenseName = "Apache License 2.0",
                licenseText = apache2,
                url = "https://developer.android.com/training/data-storage/room",
                category = LibraryCategory.DATABASE_CONCURRENCY
            ),
            ThirdPartyLib(
                name = "ZXing Core",
                author = "ZXing Authors",
                description = "Open-source barcode image processing library used to generate QR codes for sharing senseBox deep links locally on-device.",
                licenseName = "Apache License 2.0",
                licenseText = apache2,
                url = "https://github.com/zxing/zxing",
                category = LibraryCategory.UTILITIES
            ),
            ThirdPartyLib(
                name = "AndroidX Navigation Compose",
                author = "Google LLC",
                description = "Type-safe navigation component for Jetpack Compose, used for in-app routing between dashboard, details, settings, and AQI screens.",
                licenseName = "Apache License 2.0",
                licenseText = apache2,
                url = "https://developer.android.com/jetpack/compose/navigation",
                category = LibraryCategory.CORE_UI
            ),
            ThirdPartyLib(
                name = "AndroidX Core KTX & SplashScreen",
                author = "Google LLC",
                description = "Kotlin extensions for Android framework APIs and the backwards-compatible splash screen API used at app launch.",
                licenseName = "Apache License 2.0",
                licenseText = apache2,
                url = "https://developer.android.com/jetpack/androidx/releases/core",
                category = LibraryCategory.CORE_UI
            ),
            ThirdPartyLib(
                name = "AndroidX Lifecycle Compose",
                author = "Google LLC",
                description = "Lifecycle-aware coroutine scopes, ViewModel integration, and Flow collection utilities for Compose.",
                licenseName = "Apache License 2.0",
                licenseText = apache2,
                url = "https://developer.android.com/jetpack/androidx/releases/lifecycle",
                category = LibraryCategory.CORE_UI
            ),
            ThirdPartyLib(
                name = "AndroidX Activity Compose",
                author = "Google LLC",
                description = "Compose integration for Activities, providing setContent support and handling configuration changes.",
                licenseName = "Apache License 2.0",
                licenseText = apache2,
                url = "https://developer.android.com/jetpack/androidx/releases/activity",
                category = LibraryCategory.CORE_UI
            ),
            ThirdPartyLib(
                name = "AndroidX AppCompat",
                author = "Google LLC",
                description = "Backwards-compatible support library that powers the in-app language picker and per-app locale support across Android versions.",
                licenseName = "Apache License 2.0",
                licenseText = apache2,
                url = "https://developer.android.com/jetpack/androidx/releases/appcompat",
                category = LibraryCategory.CORE_UI
            ),
            ThirdPartyLib(
                name = "Compose Material Icons Extended",
                author = "Google LLC",
                description = "Extended Material Design icon set used throughout the app for richer iconography.",
                licenseName = "Apache License 2.0",
                licenseText = apache2,
                url = "https://developer.android.com/reference/kotlin/androidx/compose/material/icons/package-summary",
                category = LibraryCategory.CORE_UI
            ),
            ThirdPartyLib(
                name = "Robolectric",
                author = "Robolectric Authors",
                description = "Unit testing framework that runs Android framework code on the JVM, used for the converter and AQI test suite.",
                licenseName = "MIT License",
                licenseText = mit,
                url = "https://github.com/robolectric/robolectric",
                category = LibraryCategory.TESTING
            ),
            ThirdPartyLib(
                name = "Roborazzi",
                author = "Takahiro Menju and Contributors",
                description = "Screenshot testing toolkit for Compose used in the automated visual regression test suite.",
                licenseName = "Apache License 2.0",
                licenseText = apache2,
                url = "https://github.com/takahirom/roborazzi",
                category = LibraryCategory.TESTING
            ),
            ThirdPartyLib(
                name = "openSenseMap API",
                author = "openSenseLab gGmbH",
                description = "Open environmental sensor platform providing the core data feed, box metadata, and sensor measurements for the app.",
                licenseName = "GPL v2 / ODbL",
                licenseText = "Open environmental data powered by openSenseMap.org.",
                url = "https://opensensemap.org",
                category = LibraryCategory.DATA_SERVICES
            ),
            ThirdPartyLib(
                name = "Photon Geocoder",
                author = "komoot GmbH",
                description = "Search and reverse-geocoding service powered by OpenStreetMap data, providing privacy-friendly address-to-coordinate lookups without Google dependencies.",
                licenseName = "CC-BY-SA",
                licenseText = "Data © OpenStreetMap contributors. Search service operated by komoot GmbH.",
                url = "https://photon.komoot.io",
                category = LibraryCategory.DATA_SERVICES
            ),
            ThirdPartyLib(
                name = "Nominatim Geocoder",
                author = "OpenStreetMap Foundation",
                description = "Open-source search and reverse-geocoding engine for OpenStreetMap data, used as a secure secondary fallback for location resolution.",
                licenseName = "ODbL License",
                licenseText = "Data © OpenStreetMap contributors. OpenStreetMap® is open data, licensed under the Open Data Commons Open Database License (ODbL) by the OpenStreetMap Foundation.",
                url = "https://nominatim.org",
                category = LibraryCategory.DATA_SERVICES
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_third_party_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("third_party_back_button")
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
                .statusBarsPadding()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.about_third_party_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val groupedLibraries = remember(libraries) {
                libraries.groupBy { it.category }
            }

            LibraryCategory.entries.forEach { category ->
                val libs = groupedLibraries[category] ?: return@forEach
                val sortedLibs = remember(libs) { libs.sortedBy { it.name } }

                Text(
                    text = stringResource(category.displayNameRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                sortedLibs.forEach { lib ->
                    var expanded by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lib_card_${lib.name.replace(" ", "_").lowercase()}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        onClick = { expanded = !expanded }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = lib.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.about_library_author, lib.author),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        try {
                                            uriHandler.openUri(lib.url)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    },
                                    modifier = Modifier.size(36.dp).testTag("lib_link_${lib.name.replace(" ", "_").lowercase()}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = stringResource(R.string.cd_open_homepage),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = lib.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                SuggestionChip(
                                    onClick = { expanded = !expanded },
                                    label = {
                                        Text(
                                            text = lib.licenseName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                )

                                Text(
                                    text = if (expanded) stringResource(R.string.about_license_hide_details) else stringResource(R.string.about_license_show_license),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (expanded) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = lib.licenseText,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

