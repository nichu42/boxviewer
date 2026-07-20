package de.nichu42.boxviewer.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import de.nichu42.boxviewer.R
import java.util.Locale

/**
 * Manages the app's per-app locale preference.
 *
 * The app follows the device locale by default. If the user explicitly picks a language,
 * the choice is persisted in [app_prefs] and applied via AppCompat so it works on Android 6+.
 */
object LocaleHelper {

    private const val PREFS_NAME = "app_prefs"
    private const val KEY_APP_LANGUAGE = "app_language"

    /** Sentinel value meaning "follow the system locale". */
    const val SYSTEM_DEFAULT = ""

    /** Supported app locales for the initial launch. */
    val SUPPORTED_LOCALES = listOf(
        SupportedLocale(tag = SYSTEM_DEFAULT, displayNameRes = R.string.language_system_default),
        SupportedLocale(tag = "en", displayNameRes = R.string.language_english),
        SupportedLocale(tag = "de", displayNameRes = R.string.language_german)
    )

    data class SupportedLocale(
        val tag: String,
        val displayNameRes: Int
    ) {
        val isSystemDefault: Boolean
            get() = tag == SYSTEM_DEFAULT
    }

    /**
     * Reads the saved language tag from prefs and applies it.
     * Call this as early as possible in the app lifecycle (e.g., in MainActivity.onCreate).
     */
    fun applySavedLocale(context: Context) {
        val tag = getSavedLanguageTag(context)
        applyLocale(tag)
    }

    /**
     * Persists the user's choice and applies it immediately.
     *
     * @param tag Empty string means "follow system default"; otherwise a BCP-47 tag
     *            from [SUPPORTED_LOCALES].
     */
    fun setLanguage(context: Context, tag: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_APP_LANGUAGE, tag)
            .apply()
        applyLocale(tag)
    }

    /** Returns the saved language tag, or empty string for system default. */
    fun getSavedLanguageTag(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_APP_LANGUAGE, SYSTEM_DEFAULT)
            ?: SYSTEM_DEFAULT
    }

    /** Returns the [SupportedLocale] matching the saved preference, or system default. */
    fun getSavedLocale(context: Context): SupportedLocale {
        val tag = getSavedLanguageTag(context)
        return SUPPORTED_LOCALES.find { it.tag == tag } ?: SUPPORTED_LOCALES.first()
    }

    private fun applyLocale(tag: String) {
        val localeList = if (tag.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
