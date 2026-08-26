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
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale
import java.util.TimeZone

/**
 * Screenshot regression tests (Roborazzi record/verify mode).
 *
 * Reference images live in app/src/test/snapshots/ and are committed to the repo.
 *
 *   ./gradlew recordRoborazziDebug   regenerate references after intentional UI changes
 *   ./gradlew verifyRoborazziDebug   compare against references (runs in CI)
 *
 * References must be regenerated from the SAME platform that verifies them
 * (CI runs Linux); font rasterization differs across host operating systems.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class AppScreenshotsTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var app: Application
  private lateinit var db: SenseBoxDatabase
  private lateinit var repository: SenseBoxRepository
  private lateinit var viewModel: SenseBoxViewModel

  private val originalTimeZone = TimeZone.getDefault()
  private val originalLocale = Locale.getDefault()

  @Before
  fun setUp() {
    // Deterministic rendering: fixed clock zone and language for date/time labels.
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    Locale.setDefault(Locale.US)

    app = ApplicationProvider.getApplicationContext()
    db = SenseBoxDatabase.getDatabase(app)
    repository = SenseBoxRepository(app, db)

    // Clear and populate mock data before tests
    runBlocking(kotlinx.coroutines.Dispatchers.IO) {
      db.clearAllTables()

      val mockBox = SavedBoxEntity(
          boxId = "box_berlin",
          name = "Berlin Tiergarten Station",
          description = "Public sensor station measuring urban air quality and microclimate.",
          exposure = "outdoor",
          latitude = 52.51,
          longitude = 13.37,
          savedAt = FIXED_SEED_TIME_MS,
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
              localFetchedAt = FIXED_SEED_TIME_MS
          ),
          SensorCacheEntity(
              sensorId = "hum",
              boxId = "box_berlin",
              sensorTitle = "Relative Humidity",
              sensorUnit = "%",
              sensorType = "humidity",
              value = "58.2",
              updatedAt = "2026-06-10T12:00:00Z",
              localFetchedAt = FIXED_SEED_TIME_MS
          ),
          SensorCacheEntity(
              sensorId = "pm25",
              boxId = "box_berlin",
              sensorTitle = "PM2.5",
              sensorUnit = "µg/m³",
              sensorType = "PM2.5",
              value = "8.4",
              updatedAt = "2026-06-10T12:00:00Z",
              localFetchedAt = FIXED_SEED_TIME_MS
          ),
          SensorCacheEntity(
              sensorId = "press",
              boxId = "box_berlin",
              sensorTitle = "Air Pressure",
              sensorUnit = "hPa",
              sensorType = "pressure",
              value = "1013.2",
              updatedAt = "2026-06-10T12:00:00Z",
              localFetchedAt = FIXED_SEED_TIME_MS
          ),
          SensorCacheEntity(
              sensorId = "light",
              boxId = "box_berlin",
              sensorTitle = "Illuminance",
              sensorUnit = "lx",
              sensorType = "light",
              value = "4500.0",
              updatedAt = "2026-06-10T12:00:00Z",
              localFetchedAt = FIXED_SEED_TIME_MS
          )
      )

      db.savedBoxDao().insertSavedBox(mockBox)
      db.sensorCacheDao().insertSensors(mockSensors)
    }

    viewModel = SenseBoxViewModel(app)
    // Never hit the live geocoder network from tests; labels fall back to raw coordinates.
    viewModel.isGeocodingEnabled = false
  }

  @After
  fun tearDown() {
    TimeZone.setDefault(originalTimeZone)
    Locale.setDefault(originalLocale)
  }

  private fun snapshotPath(name: String): String = "src/test/snapshots/$name.png"

  private fun captureSnapshot(name: String) {
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().captureRoboImage(snapshotPath(name))
  }

  @Test
  fun dashboard_phone() {
    composeTestRule.setContent {
      MyApplicationTheme(dynamicColor = false) {
        DashboardScreen(
            viewModel = viewModel,
            onBoxSelected = {},
            onGoToDiscovery = {}
        )
      }
    }
    captureSnapshot("dashboard_phone")
  }

  @Test
  fun detail_phone() {
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
    captureSnapshot("detail_phone")
  }

  @Test
  fun widget_config_phone() {
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
    captureSnapshot("widget_config_phone")
  }

  @Test
  @Config(qualifiers = "w600dp-h1024dp-320dpi")
  fun dashboard_tablet7() {
    composeTestRule.setContent {
      MyApplicationTheme(dynamicColor = false) {
        DashboardScreen(
            viewModel = viewModel,
            onBoxSelected = {},
            onGoToDiscovery = {}
        )
      }
    }
    captureSnapshot("dashboard_tablet7")
  }

  @Test
  @Config(qualifiers = "w600dp-h1024dp-320dpi")
  fun detail_tablet7() {
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
    captureSnapshot("detail_tablet7")
  }

  @Test
  @Config(qualifiers = "w800dp-h1280dp-320dpi")
  fun dashboard_tablet10() {
    composeTestRule.setContent {
      MyApplicationTheme(dynamicColor = false) {
        DashboardScreen(
            viewModel = viewModel,
            onBoxSelected = {},
            onGoToDiscovery = {}
        )
      }
    }
    captureSnapshot("dashboard_tablet10")
  }

  @Test
  @Config(qualifiers = "w800dp-h1280dp-320dpi")
  fun detail_tablet10() {
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
    captureSnapshot("detail_tablet10")
  }

  private companion object {
    /** 2026-06-10T12:00:00Z — fixed so "Synced:" labels render identically on every run. */
    const val FIXED_SEED_TIME_MS = 1781092800000L
  }
}
