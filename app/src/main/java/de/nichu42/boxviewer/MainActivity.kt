package de.nichu42.boxviewer

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.nichu42.boxviewer.ui.AboutScreen
import de.nichu42.boxviewer.ui.SettingsScreen
import de.nichu42.boxviewer.ui.AqiInfoScreen
import de.nichu42.boxviewer.ui.AddBoxConfirmScreen
import de.nichu42.boxviewer.ui.LicenseScreen
import de.nichu42.boxviewer.ui.ThirdPartyLicensesScreen
import de.nichu42.boxviewer.ui.BoxDetailScreen
import de.nichu42.boxviewer.ui.DashboardScreen
import de.nichu42.boxviewer.ui.DiscoveryScreen
import de.nichu42.boxviewer.data.db.DB_VERSION
import de.nichu42.boxviewer.ui.SenseBoxViewModel
import de.nichu42.boxviewer.ui.theme.MyApplicationTheme
import de.nichu42.boxviewer.util.ApiLogger
import de.nichu42.boxviewer.data.db.SenseBoxDatabase
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    
    private val pendingBoxIdFromWidget = MutableStateFlow<String?>(null)
    private val pendingAddBoxId = MutableStateFlow<String?>(null)

    private val viewModel: SenseBoxViewModel by viewModels {
        SenseBoxViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        ApiLogger.init(applicationContext)
        enableEdgeToEdge()

        pendingBoxIdFromWidget.value = intent?.getStringExtra("box_id")

        parseDeepLinkUri(intent)?.let { boxId ->
            pendingAddBoxId.value = boxId
        }

        val dbVersion = getDatabaseVersion()
        val currentExpectedVersion = DB_VERSION
        val isDowngraded = dbVersion > currentExpectedVersion

        setContent {
            val appTheme by viewModel.appTheme.collectAsState()
            val isDarkTheme = when (appTheme) {
                SenseBoxViewModel.AppTheme.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                SenseBoxViewModel.AppTheme.LIGHT -> false
                SenseBoxViewModel.AppTheme.DARK -> true
            }
            // Apply the custom Sleek Interface theme matching Material 3 specifications
            MyApplicationTheme(darkTheme = isDarkTheme, dynamicColor = false) {
                var isDowngradedState by remember { mutableStateOf(isDowngraded) }

                if (isDowngradedState) {
                    AlertDialog(
                        onDismissRequest = { /* Prevent dismiss on click outside */ },
                        properties = androidx.compose.ui.window.DialogProperties(dismissOnClickOutside = false),
                        title = { Text("Database Version Mismatch", fontWeight = FontWeight.Bold) },
                        text = {
                            Text(
                                "The app database has been updated to a newer version. Your current app version is too old to read it.\n\n" +
                                "To ensure stability, you can choose to start over fresh (which will clear your bookmarks and widget settings), or close the app and open the project page to install the update."
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    applicationContext.deleteDatabase("sensebox_database")
                                    isDowngradedState = false
                                }
                            ) {
                                Text("Start Over")
                            }
                        },
                        dismissButton = {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, "https://codeberg.org/nichu42/BoxViewer".toUri())
                                    startActivity(intent)
                                    finish()
                                }
                            ) {
                                Text("Install Update")
                            }
                        }
                    )
                } else {
                    var showResetAlert by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                        if (prefs.getBoolean("db_reset_occurred", false)) {
                            showResetAlert = true
                            prefs.edit { putBoolean("db_reset_occurred", false) }
                        }
                    }

                    if (showResetAlert) {
                        AlertDialog(
                            onDismissRequest = { showResetAlert = false },
                            title = { Text("Database Updated", fontWeight = FontWeight.Bold) },
                            text = {
                                Text("The app database has been updated to a newer version. To ensure stability and compatibility, some configurations have been reset. Please configure your favorites and widgets again.")
                            },
                            confirmButton = {
                                Button(onClick = { showResetAlert = false }) {
                                    Text("OK")
                                }
                            }
                        )
                    }

                    val navController = rememberNavController()
                    val boxIdFromWidget by pendingBoxIdFromWidget.collectAsState()

                    LaunchedEffect(boxIdFromWidget) {
                        if (!boxIdFromWidget.isNullOrEmpty()) {
                            navController.navigate("detail/$boxIdFromWidget")
                            // Reset the state to prevent infinite loop or re-navigation on recomposition
                            pendingBoxIdFromWidget.value = null
                            intent?.removeExtra("box_id")
                        }
                    }

                    val addBoxId by pendingAddBoxId.collectAsState()

                    LaunchedEffect(addBoxId) {
                        if (!addBoxId.isNullOrEmpty()) {
                            navController.navigate("add/$addBoxId")
                            pendingAddBoxId.value = null
                            intent?.data = null
                        }
                    }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route

                        // Only render Bottom Navigation rails on root tabs (not on detailed views)
                        if (currentRoute == "dashboard" || currentRoute == "discovery" || currentRoute == "settings" || currentRoute == "about") {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                                    label = { Text("My senseBoxes", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                    selected = currentRoute == "dashboard",
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                                    ),
                                    onClick = {
                                        if (currentRoute != "dashboard") {
                                            navController.navigate("dashboard") {
                                                popUpTo("dashboard") { inclusive = true }
                                            }
                                        }
                                    }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Search, contentDescription = "Discover") },
                                    label = { Text("Discover", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                    selected = currentRoute == "discovery",
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                                    ),
                                    onClick = {
                                        if (currentRoute != "discovery") {
                                            navController.navigate("discovery") {
                                                popUpTo("dashboard")
                                            }
                                        }
                                    }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                    selected = currentRoute == "settings",
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                                    ),
                                    onClick = {
                                        if (currentRoute != "settings") {
                                            navController.navigate("settings") {
                                                popUpTo("dashboard")
                                            }
                                        }
                                    }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Info, contentDescription = "About") },
                                    label = { Text("About", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                    selected = currentRoute == "about",
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                                    ),
                                    onClick = {
                                        if (currentRoute != "about") {
                                            navController.navigate("about") {
                                                popUpTo("dashboard")
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "dashboard",
                        modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        composable("dashboard") {
                            DashboardScreen(
                                viewModel = viewModel,
                                onBoxSelected = { boxId ->
                                    navController.navigate("detail/$boxId")
                                },
                                onGoToDiscovery = {
                                    navController.navigate("discovery")
                                }
                            )
                        }
                        composable("discovery") {
                            DiscoveryScreen(
                                viewModel = viewModel,
                                onBoxSelected = { boxId ->
                                    navController.navigate("detail/$boxId")
                                },
                                onNavigateToDashboardWithConfig = { boxId ->
                                    viewModel.setAutoConfigureBox(boxId)
                                    if (navController.currentDestination?.route != "dashboard") {
                                        navController.navigate("dashboard") {
                                            popUpTo("dashboard") { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateToAqiInfo = {
                                    navController.navigate("aqi_info")
                                }
                            )
                        }
                        composable("aqi_info") {
                            AqiInfoScreen(
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable("about") {
                            AboutScreen(
                                viewModel = viewModel,
                                onViewLicense = {
                                    navController.navigate("license")
                                },
                                onViewThirdPartyLicenses = {
                                    navController.navigate("third_party_licenses")
                                }
                            )
                        }
                        composable("license") {
                            LicenseScreen(
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable("third_party_licenses") {
                            ThirdPartyLicensesScreen(
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable("detail/{boxId}") { backStackEntry ->
                            val boxId = backStackEntry.arguments?.getString("boxId") ?: ""
                            BoxDetailScreen(
                                boxId = boxId,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onNavigateToDashboardWithConfig = { boxIdVal ->
                                    viewModel.setAutoConfigureBox(boxIdVal)
                                    if (navController.currentDestination?.route != "dashboard") {
                                        navController.navigate("dashboard") {
                                            popUpTo("dashboard") { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            )
                        }
                        composable("add/{boxId}") { backStackEntry ->
                            val boxId = backStackEntry.arguments?.getString("boxId") ?: ""
                            val savedBoxesState by viewModel.savedBoxes.collectAsState()
                            var userActionTaken by remember { mutableStateOf(false) }

                            LaunchedEffect(savedBoxesState, boxId) {
                                if (!userActionTaken && savedBoxesState.any { it.boxId == boxId }) {
                                    userActionTaken = true
                                    navController.navigate("detail/$boxId") {
                                        popUpTo("add/$boxId") { inclusive = true }
                                    }
                                }
                            }

                            AddBoxConfirmScreen(
                                boxId = boxId,
                                viewModel = viewModel,
                                onAddToDashboard = { id ->
                                    userActionTaken = true
                                    viewModel.setAutoConfigureBox(id)
                                    navController.navigate("dashboard") {
                                        popUpTo("dashboard") { inclusive = false }
                                        launchSingleTop = true
                                    }
                                },
                                onViewDetails = { id ->
                                    userActionTaken = true
                                    navController.navigate("detail/$id") {
                                        popUpTo("add/$boxId") { inclusive = true }
                                    }
                                },
                                onCancel = {
                                    userActionTaken = true
                                    navController.popBackStack("dashboard", inclusive = false)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

    override fun onResume() {
        super.onResume()
        ApiLogger.isAppForeground = true
    }

    override fun onPause() {
        super.onPause()
        ApiLogger.isAppForeground = false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingBoxIdFromWidget.value = intent.getStringExtra("box_id")
        parseDeepLinkUri(intent)?.let { boxId ->
            pendingAddBoxId.value = boxId
        }
    }

    private fun parseDeepLinkUri(intent: Intent): String? {
        if (intent.action != Intent.ACTION_VIEW) return null
        val uri = intent.data ?: return null

        val id = when (uri.scheme) {
            "https" -> if (uri.host == "nichu42.codeberg.page") uri.getQueryParameter("id") else null
            "boxviewer" -> if (uri.host == "box") uri.pathSegments.firstOrNull() else null
            else -> null
        }

        if (id == null || !id.matches(Regex("^[0-9a-fA-F]{24}$"))) {
            if (id != null) {
                Toast.makeText(this, "Invalid senseBox link", Toast.LENGTH_SHORT).show()
            }
            return null
        }
        return id
    }

    private fun getDatabaseVersion(): Int {
        val dbFile = getDatabasePath("sensebox_database")
        if (!dbFile.exists()) return 0
        return try {
            SQLiteDatabase.openDatabase(
                dbFile.path,
                null,
                SQLiteDatabase.OPEN_READONLY
            ).use { db ->
                db.version
            }
        } catch (_: Exception) {
            0
        }
    }
}
