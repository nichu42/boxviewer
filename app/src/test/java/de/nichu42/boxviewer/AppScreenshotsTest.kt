package de.nichu42.boxviewer

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import de.nichu42.boxviewer.data.db.SavedBoxEntity
import de.nichu42.boxviewer.data.db.SenseBoxDatabase
import de.nichu42.boxviewer.data.db.SensorCacheEntity
import de.nichu42.boxviewer.data.repository.SenseBoxRepository
import de.nichu42.boxviewer.ui.DashboardScreen
import de.nichu42.boxviewer.ui.BoxDetailScreen
import de.nichu42.boxviewer.ui.WidgetConfigScreen
import de.nichu42.boxviewer.ui.SenseBoxViewModel
import de.nichu42.boxviewer.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class AppScreenshotsTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var app: Application
  private lateinit var db: SenseBoxDatabase
  private lateinit var repository: SenseBoxRepository
  private lateinit var viewModel: SenseBoxViewModel

  @Before
  fun setUp() {
    app = ApplicationProvider.getApplicationContext()
    db = SenseBoxDatabase.getDatabase(app)
    repository = SenseBoxRepository(app, db)
    
    // Clear and populate mock data before tests
    runBlocking(kotlinx.coroutines.Dispatchers.IO) {
      db.clearAllTables()
      
      val now = System.currentTimeMillis()
      val mockBox = SavedBoxEntity(
          boxId = "box_berlin",
          name = "Berlin Tiergarten Station",
          description = "Public sensor station measuring urban air quality and microclimate.",
          exposure = "outdoor",
          latitude = 52.51,
          longitude = 13.37,
          savedAt = now,
          dashboardSensorIds = "temp,hum,pm25,press,light"
      )
      
      val mockSensors = listOf(
          SensorCacheEntity(
              sensorId = "temp",
              boxId = "box_berlin",
              sensorTitle = "Temperature",
              sensorUnit = "°C",
              sensorType = "temperature",
              value = "21.5",
              updatedAt = "2026-06-10T12:00:00Z",
              localFetchedAt = now
          ),
          SensorCacheEntity(
              sensorId = "hum",
              boxId = "box_berlin",
              sensorTitle = "Relative Humidity",
              sensorUnit = "%",
              sensorType = "humidity",
              value = "58.2",
              updatedAt = "2026-06-10T12:00:00Z",
              localFetchedAt = now
          ),
          SensorCacheEntity(
              sensorId = "pm25",
              boxId = "box_berlin",
              sensorTitle = "PM2.5",
              sensorUnit = "µg/m³",
              sensorType = "PM2.5",
              value = "8.4",
              updatedAt = "2026-06-10T12:00:00Z",
              localFetchedAt = now
          ),
          SensorCacheEntity(
              sensorId = "press",
              boxId = "box_berlin",
              sensorTitle = "Air Pressure",
              sensorUnit = "hPa",
              sensorType = "pressure",
              value = "1013.2",
              updatedAt = "2026-06-10T12:00:00Z",
              localFetchedAt = now
          ),
          SensorCacheEntity(
              sensorId = "light",
              boxId = "box_berlin",
              sensorTitle = "Illuminance",
              sensorUnit = "lx",
              sensorType = "light",
              value = "4500.0",
              updatedAt = "2026-06-10T12:00:00Z",
              localFetchedAt = now
          )
      )
      
      db.savedBoxDao().insertSavedBox(mockBox)
      db.sensorCacheDao().insertSensors(mockSensors)
    }

    viewModel = SenseBoxViewModel(app)
  }

  @Test
  fun capture_dashboard() {
    composeTestRule.setContent {
      MyApplicationTheme(dynamicColor = false) {
        DashboardScreen(
            viewModel = viewModel,
            onBoxSelected = {},
            onGoToDiscovery = {}
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage(
        filePath = "C:/Users/tee3/.gemini/antigravity-ide/brain/228aac2f-3761-4174-9974-e8e8212de6e1/screenshot_dashboard.png"
    )
  }

  @Test
  fun capture_detail() {
    composeTestRule.setContent {
      MyApplicationTheme(dynamicColor = false) {
        BoxDetailScreen(
            boxId = "box_berlin",
            viewModel = viewModel,
            onBack = {},
            onNavigateToDashboardWithConfig = {}
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage(
        filePath = "C:/Users/tee3/.gemini/antigravity-ide/brain/228aac2f-3761-4174-9974-e8e8212de6e1/screenshot_detail.png"
    )
  }

  @Test
  fun capture_widget_config() {
    composeTestRule.setContent {
      MyApplicationTheme(dynamicColor = false) {
        WidgetConfigScreen(
            repository = repository,
            appWidgetId = 1,
            onConfigSaved = {},
            onConfigCancelled = {}
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage(
        filePath = "C:/Users/tee3/.gemini/antigravity-ide/brain/228aac2f-3761-4174-9974-e8e8212de6e1/screenshot_widget_config.png"
    )
  }

  @Test
  @Config(qualifiers = "w600dp-h1024dp-320dpi")
  fun capture_dashboard_7inch() {
    composeTestRule.setContent {
      MyApplicationTheme(dynamicColor = false) {
        DashboardScreen(
            viewModel = viewModel,
            onBoxSelected = {},
            onGoToDiscovery = {}
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage(
        filePath = "C:/Users/tee3/.gemini/antigravity-ide/brain/228aac2f-3761-4174-9974-e8e8212de6e1/screenshot_dashboard_7inch.png"
    )
  }

  @Test
  @Config(qualifiers = "w600dp-h1024dp-320dpi")
  fun capture_detail_7inch() {
    composeTestRule.setContent {
      MyApplicationTheme(dynamicColor = false) {
        BoxDetailScreen(
            boxId = "box_berlin",
            viewModel = viewModel,
            onBack = {},
            onNavigateToDashboardWithConfig = {}
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage(
        filePath = "C:/Users/tee3/.gemini/antigravity-ide/brain/228aac2f-3761-4174-9974-e8e8212de6e1/screenshot_detail_7inch.png"
    )
  }

  @Test
  @Config(qualifiers = "w800dp-h1280dp-320dpi")
  fun capture_dashboard_10inch() {
    composeTestRule.setContent {
      MyApplicationTheme(dynamicColor = false) {
        DashboardScreen(
            viewModel = viewModel,
            onBoxSelected = {},
            onGoToDiscovery = {}
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage(
        filePath = "C:/Users/tee3/.gemini/antigravity-ide/brain/228aac2f-3761-4174-9974-e8e8212de6e1/screenshot_dashboard_10inch.png"
    )
  }

  @Test
  @Config(qualifiers = "w800dp-h1280dp-320dpi")
  fun capture_detail_10inch() {
    composeTestRule.setContent {
      MyApplicationTheme(dynamicColor = false) {
        BoxDetailScreen(
            boxId = "box_berlin",
            viewModel = viewModel,
            onBack = {},
            onNavigateToDashboardWithConfig = {}
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage(
        filePath = "C:/Users/tee3/.gemini/antigravity-ide/brain/228aac2f-3761-4174-9974-e8e8212de6e1/screenshot_detail_10inch.png"
    )
  }
}
