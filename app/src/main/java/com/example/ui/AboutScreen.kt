package com.example.ui

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.io.BufferedReader
import java.io.InputStreamReader
import com.example.R
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import com.example.util.CrashHandler
import com.example.util.ApiLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onViewLicense: () -> Unit = {},
    onViewThirdPartyLicenses: () -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var crashLog by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        crashLog = CrashHandler.getCrashLog(context)
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
        } catch (e: Exception) {
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
                contentDescription = "BoxViewer Logo",
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(36.dp)),
                contentScale = ContentScale.Fit
            )

            Text(
                text = "Version $versionName",
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
                        text = "APP HOME & SUPPORT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "BoxViewer is open-source and developed with love. Check out the project repository to contribute, report issues, or view the source code:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Codeberg Link Button
                    Button(
                        onClick = {
                            try {
                                uriHandler.openUri("https://codeberg.org/nichu42/BoxViewer")
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
                            contentDescription = "Codeberg Homepage",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Project Homepage (Codeberg)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Support the Developer",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "If you are happy with the app and would like to support the ongoing development, please consider donating:",
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
                                contentDescription = "Ko-fi",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Support on Ko-fi",
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
                                contentDescription = "Liberapay",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Donate via Liberapay",
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
                        text = "Copyright (C) 2026 nichu42 and contributors",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.",
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
                            contentDescription = "GPL v3 Logo",
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
                            contentDescription = "License",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "View Full License",
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
                            contentDescription = "Third-Party Licenses",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Third-Party Licenses",
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
                        text = "DATA & ATTRIBUTION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "This app utilizes the open API provided by openSenseMap, an open-source platform dedicated to collecting and exploring environmental sensor data from around the globe.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "What is openSenseMap?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Originally emerged from a research project at the University of Münster (Germany), openSenseMap has grown into one of the largest citizen-operated sensor networks in the world. It provides a free platform for schools, universities, scientists, and citizen enthusiasts to publish real-time environmental measurements—such as air quality, temperature, and humidity—and share them as Open Data.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Who operates it?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "The platform is operated and maintained by openSenseLab gGmbH, a non-profit organization based in Münster, Germany, dedicated to promoting digital sovereignty, education, and public participation in scientific environmental monitoring.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Support Open Data!",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "openSenseMap is completely free to use and relies heavily on community contributions and donations to keep its servers running and its data accessible to all. If you love the environmental insights provided in this app, please consider supporting their project:",
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
                                contentDescription = "Website",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Explore: opensensemap.org",
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
                                contentDescription = "Build",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Build: sensebox.de",
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
                                contentDescription = "Donate",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Donate via Betterplace",
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
                            contentDescription = "Attention Warning",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AFFILIATION DISCLAIMER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "The BoxViewer app is an independent project and is not affiliated with, endorsed by, or connected to openSenseMap (openSenseLab gGmbH) or senseBox (Reedu GmbH & Co. KG) in any way.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
            
            // 5. Diagnostics & Bug Reporting Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("about_diagnostics_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "DIAGNOSTICS & BUG REPORTING",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    if (crashLog != null) {
                        Text(
                            text = "⚠️ Detection: A crash was captured from a recent session. Please copy the log below and include it in your issue on Codeberg.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            Text(
                                text = crashLog ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    crashLog?.let {
                                        clipboardManager.setText(AnnotatedString(it))
                                        Toast.makeText(context, "Crash report copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            ) {
                                Text("Copy Crash Log", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = {
                                    CrashHandler.clearCrashLog(context)
                                    crashLog = null
                                    Toast.makeText(context, "Crash log cleared", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            ) {
                                Text("Clear Log", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else {
                        Text(
                            text = "No crashes detected. The app is running smoothly! If you encounter any bugs, you can copy standard system info to help us diagnose the issue.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                val systemInfo = CrashHandler.generateSystemDiagnostics(context)
                                clipboardManager.setText(AnnotatedString(systemInfo))
                                Toast.makeText(context, "Diagnostics copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = "Copy diagnostics", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copy System Diagnostics Info", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. API Debug Logging Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("about_api_logging_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "API DEBUG LOGGING",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text(
                        text = "Capture raw JSON network requests, responses, and internal parsing metrics. Logs are saved locally in a JSON Lines format and can be shared to diagnose issues.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    var apiLoggingEnabled by remember { mutableStateOf(ApiLogger.isLoggingEnabled()) }
                    var apiLogLimit by remember { mutableStateOf(ApiLogger.getMaxEntries()) }
                    var logSizeStr by remember { mutableStateOf("0 B") }

                    LaunchedEffect(apiLoggingEnabled) {
                        val size = ApiLogger.getLogFileSize()
                        logSizeStr = formatFileSize(size)
                    }

                    // Logging Toggle Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Enable API Logging",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Current size: $logSizeStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = apiLoggingEnabled,
                            onCheckedChange = { checked ->
                                apiLoggingEnabled = checked
                                ApiLogger.setLoggingEnabled(checked)
                                Toast.makeText(context, if (checked) "API Logging Enabled" else "API Logging Disabled", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("api_logging_toggle")
                        )
                    }

                    if (apiLoggingEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Max Log Entries",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Limit Selector Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(50, 100, 200, 500).forEach { limitVal ->
                                val isSelected = apiLogLimit == limitVal
                                Button(
                                    onClick = {
                                        apiLogLimit = limitVal
                                        ApiLogger.setMaxEntries(limitVal)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .testTag("api_limit_$limitVal"),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(text = "$limitVal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons: Copy, Share, Clear
                        val coroutineScope = rememberCoroutineScope()
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val logText = ApiLogger.getLogsText()
                                            if (logText.isEmpty()) {
                                                Toast.makeText(context, "Log is empty!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                clipboardManager.setText(AnnotatedString(logText))
                                                Toast.makeText(context, "Logs copied to clipboard!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("copy_api_logs")
                                ) {
                                    Text("Copy Logs", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val logText = ApiLogger.getLogsText()
                                            if (logText.isEmpty()) {
                                                Toast.makeText(context, "Log is empty!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                try {
                                                    val sendIntent = android.content.Intent().apply {
                                                        action = android.content.Intent.ACTION_SEND
                                                        putExtra(android.content.Intent.EXTRA_TEXT, logText)
                                                        type = "text/plain"
                                                    }
                                                    val shareIntent = android.content.Intent.createChooser(sendIntent, "Share API Logs")
                                                    context.startActivity(shareIntent)
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    Toast.makeText(context, "Error sharing logs", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("share_api_logs")
                                ) {
                                    Text("Share Logs", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Button(
                                onClick = {
                                    ApiLogger.clearLogs()
                                    logSizeStr = "0 B"
                                    Toast.makeText(context, "Logs cleared", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("clear_api_logs")
                            ) {
                                Text("Clear API Logs", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
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
    val licenseText = remember {
        try {
            val inputStream = context.resources.openRawResource(R.raw.gpl_license)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val text = reader.readText()
            reader.close()
            normalizeLicenseText(text)
        } catch (e: Exception) {
            "Error loading license text."
        }
    }
    
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GNU General Public License", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("license_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
        val thumbHeightPx = with(LocalDensity.current) { thumbHeightDp.toPx() }
        
        val scrollFraction = if (scrollState.maxValue > 0) {
            scrollState.value.toFloat() / scrollState.maxValue.toFloat()
        } else {
            0f
        }
        
        val trackHeightPx = maxHeightPx - thumbHeightPx
        val thumbOffsetPx = scrollFraction * trackHeightPx
        
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
                            val delta = targetScroll.toFloat() - scrollState.value.toFloat()
                            scrollState.dispatchRawDelta(delta)
                        }
                    }
                }
        ) {
            // Interactive Dragable Scrollbar Thumb
            Box(
                modifier = Modifier
                    .offset(y = with(LocalDensity.current) { thumbOffsetPx.toDp() })
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

// Custom Third Party Licenses Screen (Option B) - 100% stable, custom styled, robust
data class ThirdPartyLib(
    val name: String,
    val author: String,
    val description: String,
    val licenseName: String,
    val licenseText: String,
    val url: String
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

    val libraries = remember {
        listOf(
            ThirdPartyLib(
                name = "Jetpack Compose & AndroidX",
                author = "Google LLC",
                description = "Modern and declarative UI toolkit, components, life cycle systems, and core tools used to build high-end Android products.",
                licenseName = "Apache License 2.0",
                licenseText = apache2,
                url = "https://developer.android.com/jetpack/compose"
            ),
            ThirdPartyLib(
                name = "Retrofit",
                author = "Square, Inc.",
                description = "A type-safe HTTP client for Android and JVM, used to translate openSenseMap REST APIs into clean Kotlin interfaces.",
                licenseName = "Apache License 2.0",
                licenseText = apache2,
                url = "https://github.com/square/retrofit"
            ),
            ThirdPartyLib(
                name = "OkHttp Engine & Interceptor",
                author = "Square, Inc.",
                description = "Efficient modern connection multiplexing HTTP logging HTTP client infrastructure.",
                licenseName = "Apache License 2.0",
                licenseText = apache2,
                url = "https://github.com/square/okhttp"
            ),
            ThirdPartyLib(
                name = "Moshi Core & Codegen",
                author = "Square, Inc.",
                description = "Modern JSON library for Android, Kotlin, and Java, facilitating rapid type-safe JSON serialization/deserialization.",
                licenseName = "Apache License 2.0",
                licenseText = apache2,
                url = "https://github.com/square/moshi"
            ),
            ThirdPartyLib(
                name = "Coil Image Loader",
                author = "Coil Contributors",
                description = "An image loading library for Android backed by Kotlin Coroutines, used to load remote assets and SVG vectors.",
                licenseName = "Apache License 2.0",
                licenseText = apache2,
                url = "https://github.com/coil-kt/coil"
            ),
            ThirdPartyLib(
                name = "Kotlinx Coroutines & Flow",
                author = "JetBrains s.r.o.",
                description = "Library support for Kotlin coroutines, facilitating clean asynchronous reactive data programming and state flow bindings.",
                licenseName = "Apache License 2.0",
                licenseText = apache2,
                url = "https://github.com/Kotlin/kotlinx.coroutines"
            ),
            ThirdPartyLib(
                name = "Room Persistence Database",
                author = "Google LLC",
                description = "Abstraction layer over SQLite, enabling robust, offline-capable structured query caching of user-added environmental sensor boxes.",
                licenseName = "Apache License 2.0",
                licenseText = apache2,
                url = "https://developer.android.com/training/data-storage/room"
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Third-Party Licenses", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("third_party_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "We are deeply grateful for the talented creators and communities behind these open-source dependencies. They help make BoxViewer lightweight, robust, and beautiful.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            libraries.forEach { lib ->
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
                                    text = "by ${lib.author}",
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
                                    contentDescription = "Open Homepage",
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
                                text = if (expanded) "Hide details" else "Show license",
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

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f KB", size / 1024.0)
        else -> String.format(java.util.Locale.US, "%.1f MB", size / (1024.0 * 1024.0))
    }
}
