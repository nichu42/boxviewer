package de.nichu42.boxviewer.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import de.nichu42.boxviewer.R

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
        SupportedLocale(tag = "de", displayNameRes = R.string.language_german),
        SupportedLocale(tag = "cs", displayNameRes = R.string.language_czech),
        SupportedLocale(tag = "fr", displayNameRes = R.string.language_french),
        SupportedLocale(tag = "hu", displayNameRes = R.string.language_hungarian),
        SupportedLocale(tag = "it", displayNameRes = R.string.language_italian),
        SupportedLocale(tag = "nl", displayNameRes = R.string.language_dutch),
        SupportedLocale(tag = "pl", displayNameRes = R.string.language_polish)
    )

    data class SupportedLocale(
        val tag: String,
        val displayNameRes: Int
    )

    /**
     * Reads the saved language tag from prefs and applies it.
     * Call this as early as possible in the app lifecycle (e.g., in MainActivity.onCreate).
     */
    fun applySavedLocale(context: Context) {
        val tag = getSavedLanguageTag(context)
        applyLocale(tag)
    }

    /**
     * Persists the user's choice, applies it immediately, and recreates the
     * current activity so the new locale is visible right away.
     *
     * @param tag Empty string means "follow system default"; otherwise a BCP-47 tag
     *            from [SUPPORTED_LOCALES].
     */
    fun setLanguage(context: Context, tag: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit(commit = true) {
                putString(KEY_APP_LANGUAGE, tag)
            }
        applyLocale(tag)
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            context.findActivity()?.recreate()
        }
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

    /** Walks up the [ContextWrapper] chain to find the hosting [Activity]. */
    private fun Context.findActivity(): Activity? {
        var currentContext = this
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) return currentContext
            currentContext = currentContext.baseContext
        }
        return null
    }
}
