package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.AboutScreen
import com.example.ui.LicenseScreen
import com.example.ui.ThirdPartyLicensesScreen
import com.example.ui.BoxDetailScreen
import com.example.ui.DashboardScreen
import com.example.ui.DiscoveryScreen
import com.example.ui.SenseBoxViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    
    private val pendingBoxIdFromWidget = MutableStateFlow<String?>(null)

    private val viewModel: SenseBoxViewModel by viewModels {
        SenseBoxViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.util.ApiLogger.init(applicationContext)
        enableEdgeToEdge()

        pendingBoxIdFromWidget.value = intent?.getStringExtra("box_id")

        val dbVersion = getDatabaseVersion()
        val currentExpectedVersion = 5
        val isDowngraded = dbVersion > currentExpectedVersion

        setContent {
            // Apply the custom Sleek Interface theme matching Material 3 specifications
            MyApplicationTheme(dynamicColor = false) {
                var isDowngradedState by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(isDowngraded) }

                if (isDowngradedState) {
                    AlertDialog(
                        onDismissRequest = { /* Prevent dismiss on click outside */ },
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
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://codeberg.org/nichu42/BoxViewer"))
                                    startActivity(intent)
                                    finish()
                                }
                            ) {
                                Text("Install Update")
                            }
                        }
                    )
                } else {
                    var showResetAlert by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        val prefs = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                        if (prefs.getBoolean("db_reset_occurred", false)) {
                            showResetAlert = true
                            prefs.edit().putBoolean("db_reset_occurred", false).apply()
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

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route

                        // Only render Bottom Navigation rails on root tabs (not on detailed views)
                        if (currentRoute == "dashboard" || currentRoute == "discovery" || currentRoute == "about") {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
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
                        modifier = Modifier.padding(innerPadding)
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
                        composable("about") {
                            AboutScreen(
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
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.example.util.ApiLogger.isAppForeground = true
    }

    override fun onPause() {
        super.onPause()
        com.example.util.ApiLogger.isAppForeground = false
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingBoxIdFromWidget.value = intent.getStringExtra("box_id")
    }

    private fun getDatabaseVersion(): Int {
        val dbFile = getDatabasePath("sensebox_database")
        if (!dbFile.exists()) return 0
        return try {
            android.database.sqlite.SQLiteDatabase.openDatabase(
                dbFile.path,
                null,
                android.database.sqlite.SQLiteDatabase.OPEN_READONLY
            ).use { db ->
                db.version
            }
        } catch (e: Exception) {
            0
        }
    }
}
