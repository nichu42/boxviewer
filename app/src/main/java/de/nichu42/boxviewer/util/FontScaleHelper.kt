package de.nichu42.boxviewer.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * Manages a user-defined app-wide text scale factor persisted in [app_prefs].
 *
 * This scales only text (`sp`), not layout (`dp`), by wrapping the composition
 * in a modified [LocalDensity] that multiplies the system's [fontScale].
 */
object FontScaleHelper {

    private const val PREFS_NAME = "app_prefs"
    private const val KEY_APP_TEXT_SCALE = "app_text_scale"

    /** Default text scale factor. */
    const val DEFAULT_SCALE = 1.0f

    /** Allowed range for the user-adjustable scale. */
    val VALUE_RANGE = 0.8f..2.0f

    /** Returns the persisted text scale, or [DEFAULT_SCALE] if none is set. */
    fun getSavedTextScale(context: Context): Float {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getFloat(KEY_APP_TEXT_SCALE, DEFAULT_SCALE)
            .coerceIn(VALUE_RANGE)
    }

    /** Persists the user's text scale choice. */
    fun setTextScale(context: Context, scale: Float) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_APP_TEXT_SCALE, scale.coerceIn(VALUE_RANGE))
            .apply()
    }

    /**
     * Wraps [content] in a [LocalDensity] whose font scale is multiplied by
     * [scale]. Use this at the root of the activity composition.
     */
    @Composable
    fun ApplyTextScale(scale: Float = DEFAULT_SCALE, content: @Composable () -> Unit) {
        val currentDensity = LocalDensity.current
        val scaledDensity = remember(currentDensity, scale) {
            Density(currentDensity.density, currentDensity.fontScale * scale.coerceIn(VALUE_RANGE))
        }
        CompositionLocalProvider(LocalDensity provides scaledDensity) {
            content()
        }
    }
}
