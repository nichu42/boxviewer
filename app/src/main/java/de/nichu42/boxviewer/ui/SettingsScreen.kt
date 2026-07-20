package de.nichu42.boxviewer.ui

import de.nichu42.boxviewer.util.AqiSystem

import android.content.ClipData
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import de.nichu42.boxviewer.R
import de.nichu42.boxviewer.util.ApiLogger
import de.nichu42.boxviewer.util.CrashHandler
import de.nichu42.boxviewer.util.FontScaleHelper
import de.nichu42.boxviewer.util.LocaleHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SenseBoxViewModel,
    onNavigateToAqiInfo: () -> Unit,
    onNavigateToApiLogViewer: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val clipboardScope = rememberCoroutineScope()
    var crashLog by remember { mutableStateOf<String?>(null) }
    val useConditionalFormatting by viewModel.useConditionalFormatting.collectAsStateWithLifecycle()
    val temperatureUnit by viewModel.temperatureUnit.collectAsStateWithLifecycle()
    val pressureUnit by viewModel.pressureUnit.collectAsStateWithLifecycle()
    val windUnit by viewModel.windUnit.collectAsStateWithLifecycle()
    val formatPressure by viewModel.formatPressure.collectAsStateWithLifecycle()
    val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val aqiSystem by viewModel.aqiSystem.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        crashLog = CrashHandler.getCrashLog(context)
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
            // GENERAL APP SETTINGS SECTION
            Card(
                modifier = Modifier.fillMaxWidth().testTag("settings_general_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        stringResource(R.string.settings_section_app_settings),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(R.string.settings_app_theme_label),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.settings_app_theme_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Theme Selector Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SenseBoxViewModel.AppTheme.entries.forEach { themeOption ->
                                val isSelected = appTheme == themeOption
                                Button(
                                    onClick = { viewModel.setAppTheme(themeOption) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .testTag("theme_${themeOption.name.lowercase()}"),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = stringResource(
                                            when (themeOption) {
                                                SenseBoxViewModel.AppTheme.SYSTEM -> R.string.settings_theme_system
                                                SenseBoxViewModel.AppTheme.LIGHT -> R.string.settings_theme_light
                                                SenseBoxViewModel.AppTheme.DARK -> R.string.settings_theme_dark
                                            }
                                        ),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Language picker
                    var currentLocale by remember { mutableStateOf(LocaleHelper.getSavedLocale(context)) }
                    var languageExpanded by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_language_label),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                stringResource(R.string.settings_language_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                            OutlinedButton(
                                onClick = { languageExpanded = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(stringResource(currentLocale.displayNameRes), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = languageExpanded,
                                onDismissRequest = { languageExpanded = false }
                            ) {
                                LocaleHelper.SUPPORTED_LOCALES.forEach { locale ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(locale.displayNameRes)) },
                                        onClick = {
                                            LocaleHelper.setLanguage(context, locale.tag)
                                            currentLocale = locale
                                            languageExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // App-wide text scale
                    var textScale by remember { mutableFloatStateOf(FontScaleHelper.getSavedTextScale(context)) }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.settings_text_scale_label),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    stringResource(R.string.settings_text_scale_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = String.format(java.util.Locale.getDefault(), "%.0f%%", textScale * 100),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = textScale,
                            onValueChange = { textScale = it },
                            onValueChangeFinished = {
                                FontScaleHelper.setTextScale(context, textScale)
                                (context as? android.app.Activity)?.recreate()
                            },
                            valueRange = FontScaleHelper.VALUE_RANGE,
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
                                stringResource(R.string.settings_text_scale_compact),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                stringResource(R.string.settings_text_scale_default),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                stringResource(R.string.settings_text_scale_large),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_conditional_formatting_label),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                stringResource(R.string.settings_conditional_formatting_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useConditionalFormatting,
                            onCheckedChange = { viewModel.setUseConditionalFormatting(it) },
                            modifier = Modifier.testTag("conditional_formatting_toggle")
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    SettingsDropdownRow(
                        label = stringResource(R.string.settings_aqi_standard_label),
                        description = stringResource(R.string.settings_aqi_standard_description),
                        options = AqiSystem.entries,
                        selectedOption = aqiSystem,
                        optionToString = { it.label },
                        testTag = "aqi_standard_dropdown",
                        onOptionSelected = { system -> viewModel.setAqiSystem(system) },
                        onInfoClick = onNavigateToAqiInfo
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    SettingsDropdownRow(
                        label = stringResource(R.string.settings_temperature_unit_label),
                        description = stringResource(R.string.settings_temperature_unit_description),
                        options = listOf(
                            stringResource(R.string.unit_celsius),
                            stringResource(R.string.unit_fahrenheit),
                            stringResource(R.string.unit_kelvin)
                        ),
                        selectedOption = temperatureUnit,
                        testTag = "temperature_unit_dropdown",
                        onOptionSelected = { viewModel.setTemperatureUnit(it) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    SettingsDropdownRow(
                        label = stringResource(R.string.settings_pressure_unit_label),
                        description = stringResource(R.string.settings_pressure_unit_description),
                        options = listOf(
                            stringResource(R.string.unit_hpa),
                            stringResource(R.string.unit_pa),
                            stringResource(R.string.unit_mbar),
                            stringResource(R.string.unit_inhg),
                            stringResource(R.string.unit_mmhg)
                        ),
                        selectedOption = pressureUnit,
                        testTag = "pressure_unit_dropdown",
                        onOptionSelected = { viewModel.setPressureUnit(it) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_format_pressure_label),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                stringResource(R.string.settings_format_pressure_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = formatPressure,
                            onCheckedChange = { viewModel.setFormatPressure(it) },
                            modifier = Modifier.testTag("format_pressure_toggle")
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    SettingsDropdownRow(
                        label = stringResource(R.string.settings_wind_unit_label),
                        description = stringResource(R.string.settings_wind_unit_description),
                        options = listOf(
                            stringResource(R.string.unit_ms),
                            stringResource(R.string.unit_kmh),
                            stringResource(R.string.unit_mph),
                            stringResource(R.string.unit_kn)
                        ),
                        selectedOption = windUnit,
                        testTag = "wind_unit_dropdown",
                        onOptionSelected = { viewModel.setWindUnit(it) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // DIAGNOSTICS & BUG REPORTING CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_diagnostics_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_section_diagnostics),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (crashLog != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = stringResource(R.string.cd_crash_detected_warning),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.settings_crash_detected_title),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
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
                                    clipboardScope.launch {
                                        clipboard.setClipEntry(ClipData.newPlainText("Crash report", it).toClipEntry())
                                    }
                                        Toast.makeText(context, context.getString(R.string.settings_crash_report_copied), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("copy_crash_log")
                            ) {
                                Text(stringResource(R.string.settings_copy_crash_log), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = {
                                    CrashHandler.clearCrashLog(context)
                                    crashLog = null
                                    Toast.makeText(context, context.getString(R.string.settings_crash_log_cleared), Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("clear_crash_log")
                            ) {
                                Text(stringResource(R.string.settings_clear_log), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.settings_no_crashes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                val systemInfo = CrashHandler.generateSystemDiagnostics(context)
                            clipboardScope.launch {
                                clipboard.setClipEntry(ClipData.newPlainText("Diagnostics", systemInfo).toClipEntry())
                            }
                                Toast.makeText(context, context.getString(R.string.settings_diagnostics_copied), Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("copy_diagnostics_info")
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = stringResource(R.string.settings_copy_diagnostics_cd), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_copy_system_diagnostics), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // API DEBUG LOGGING CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_api_logging_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_section_api_logging),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text(
                        text = stringResource(R.string.settings_api_logging_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    var apiLoggingEnabled by remember { mutableStateOf(ApiLogger.isLoggingEnabled()) }
                    var apiLogLimit by remember { mutableIntStateOf(ApiLogger.getMaxEntries()) }
                    var logSizeStr by remember { mutableStateOf("0 B") }

                    LaunchedEffect(apiLoggingEnabled) {
                        val size = ApiLogger.getLogFileSize()
                        logSizeStr = formatSettingsFileSize(context, size)
                    }

                    // Logging Toggle Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.settings_enable_api_logging),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.settings_current_size_format, logSizeStr),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = apiLoggingEnabled,
                            onCheckedChange = { checked ->
                                apiLoggingEnabled = checked
                                ApiLogger.setLoggingEnabled(checked)
                                Toast.makeText(
                                    context,
                                    if (checked) context.getString(R.string.settings_api_logging_enabled) else context.getString(R.string.settings_api_logging_disabled),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.testTag("api_logging_toggle")
                        )
                    }

                    if (apiLoggingEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.settings_max_log_entries),
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
                            Button(
                                onClick = {
                                    onNavigateToApiLogViewer()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("view_api_logs")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.List,
                                    contentDescription = stringResource(R.string.cd_view_api_logs),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.settings_view_api_logs), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val logText = ApiLogger.getLogsText()
                                            if (logText.isEmpty()) {
                                                Toast.makeText(context, context.getString(R.string.settings_log_empty), Toast.LENGTH_SHORT).show()
                                            } else {
                                                clipboard.setClipEntry(ClipData.newPlainText("API Logs", logText).toClipEntry())
                                                Toast.makeText(context, context.getString(R.string.settings_logs_copied), Toast.LENGTH_SHORT).show()
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
                                    Text(stringResource(R.string.settings_copy_logs), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val logText = ApiLogger.getLogsText()
                                            if (logText.isEmpty()) {
                                                Toast.makeText(context, context.getString(R.string.settings_log_empty), Toast.LENGTH_SHORT).show()
                                            } else {
                                                try {
                                                    val sendIntent = android.content.Intent().apply {
                                                        action = android.content.Intent.ACTION_SEND
                                                        putExtra(android.content.Intent.EXTRA_TEXT, logText)
                                                        type = "text/plain"
                                                    }
                                                    val shareIntent = android.content.Intent.createChooser(
                                                        sendIntent,
                                                        context.getString(R.string.settings_share_api_logs_title)
                                                    )
                                                    context.startActivity(shareIntent)
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    Toast.makeText(context, context.getString(R.string.settings_error_sharing_logs), Toast.LENGTH_SHORT).show()
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
                                    Text(stringResource(R.string.settings_share_logs), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Button(
                                onClick = {
                                    ApiLogger.clearLogs()
                                    logSizeStr = "0 B"
                                    Toast.makeText(context, context.getString(R.string.settings_logs_cleared), Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("clear_api_logs")
                            ) {
                                Text(stringResource(R.string.settings_clear_api_logs), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

fun formatSettingsFileSize(context: Context, size: Long): String {
    return when {
        size < 1024 -> context.getString(R.string.file_size_bytes, size)
        size < 1024 * 1024 -> context.getString(R.string.file_size_kilobytes, size / 1024.0)
        else -> context.getString(R.string.file_size_megabytes, size / (1024.0 * 1024.0))
    }
}

@Composable
private fun <T> SettingsDropdownRow(
    label: String,
    description: String,
    options: List<T>,
    selectedOption: T,
    optionToString: (T) -> String = { it.toString() },
    testTag: String,
    onOptionSelected: (T) -> Unit,
    onInfoClick: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                if (onInfoClick != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onInfoClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.cd_info),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.testTag(testTag),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(optionToString(selectedOption), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionToString(option), style = MaterialTheme.typography.bodyMedium) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
