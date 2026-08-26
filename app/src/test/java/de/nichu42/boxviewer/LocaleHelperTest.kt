package de.nichu42.boxviewer

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import de.nichu42.boxviewer.data.api.Measurement
import de.nichu42.boxviewer.data.api.SenseBox
import de.nichu42.boxviewer.data.api.Sensor
import de.nichu42.boxviewer.ui.SenseBoxViewModel
import de.nichu42.boxviewer.util.LocaleHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression tests for the per-app language subsystem.
 *
 * The device locale is forced to German via Robolectric qualifiers while the tests
 * select English as the in-app language. This reproduces the historical bug where
 * ViewModels formatted user-facing strings through the raw Application context,
 * which AppCompatDelegate does NOT localize on API < 33, and where a per-box
 * text cache survived language switches (ViewModels outlive activity recreation).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "de")
class LocaleHelperTest {

    private val app: Application = ApplicationProvider.getApplicationContext()
    private lateinit var viewModel: SenseBoxViewModel

    private val prefs get() = app.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    @Before
    fun setUp() {
        prefs.edit { clear() }
        viewModel = SenseBoxViewModel(app)
    }

    @After
    fun tearDown() {
        prefs.edit { clear() }
    }

    private fun setAppLanguage(tag: String) {
        // Write the preference directly; setLanguage() would try to recreate an activity.
        prefs.edit { putString("app_language", tag) }
    }

    @Test
    fun localizedContext_resolvesSelectedLanguage_onMismatchedDeviceLocale() {
        setAppLanguage("en")

        val ctx = LocaleHelper.getLocalizedContext(app)
        assertEquals("Last Updated", ctx.getString(R.string.cd_last_updated))
        assertEquals("Last updated: %1\$s", ctx.getString(R.string.last_updated_format))

        // Sanity: the raw application context still resolves the device locale (German),
        // which is exactly why ViewModels must use the localized wrapper.
        assertEquals("Zuletzt aktualisiert", app.getString(R.string.cd_last_updated))
    }

    @Test
    fun localizedContext_followsDeviceLocale_whenSystemDefaultSelected() {
        setAppLanguage(LocaleHelper.SYSTEM_DEFAULT)

        val ctx = LocaleHelper.getLocalizedContext(app)
        assertEquals("Zuletzt aktualisiert", ctx.getString(R.string.cd_last_updated))
    }

    @Test
    fun formatLastUpdated_switchesLanguageImmediately_withoutStaleCache() {
        val box = testBox()

        setAppLanguage("de")
        val germanText = viewModel.formatLastUpdated(box)
        assertTrue("Expected German text, got: $germanText", germanText.contains("Zuletzt aktualisiert"))

        // User switches language mid-session: the ViewModel survives activity recreation,
        // so the text cache must not keep serving the old language.
        setAppLanguage("en")
        val englishText = viewModel.formatLastUpdated(box)
        assertTrue("Expected English text after switch, got: $englishText", englishText.contains("Last updated"))
        assertFalse(englishText.contains("Zuletzt"))
    }

    @Test
    fun formatAppSyncTime_usesLocalizedNeverLabel() {
        setAppLanguage("en")
        // Device locale is German ("Nie"); the ViewModel must resolve the English label.
        assertEquals("Never", viewModel.formatAppSyncTime(emptyList()))
    }

    private fun testBox() = SenseBox(
        id = "box_locale_test",
        name = "Locale Test Box",
        description = null,
        exposure = "outdoor",
        model = null,
        grouptagRaw = null,
        currentLocation = null,
        sensors = listOf(
            Sensor(
                id = "sensor_temp",
                title = "Temperature",
                unit = "°C",
                sensorType = "temperature",
                lastMeasurement = Measurement(value = "21.5", createdAt = "2026-06-10T12:00:00Z")
            )
        )
    )
}
