package de.nichu42.boxviewer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.test.core.app.ApplicationProvider
import de.nichu42.boxviewer.widget.SenseBoxWidgetProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Regression guard for the 0.53 → 0.54 "Can't load widget" (Android 12+, esp. 14).
 *
 * Two failure modes are locked:
 * 1. Any RemoteViews action used by the provider must be @RemotableViewMethod —
 *    otherwise the launcher throws on apply() and shows the host error.
 * 2. The provider must not silently drop updates (missing config / DB error) and
 *    must hold the broadcast alive with goAsync() so an async update isn't reaped.
 *
 * The first is verified by actually applying RemoteViews that mirror the provider's
 * action sets. The second is verified structurally (source contains goAsync + fallback).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetRemoteViewsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `list layout RemoteViews apply with provider action set does not throw`() {
        val views = RemoteViews(context.packageName, R.layout.widget_layout_list)

        // Header — mirrors SenseBoxWidgetProvider.buildRemoteViews header block
        views.setViewVisibility(R.id.widget_header, View.VISIBLE)
        views.setTextViewText(R.id.widget_box_name, "Test Box")
        views.setViewVisibility(R.id.widget_box_name, View.VISIBLE)
        views.setTextViewText(R.id.widget_update_time, "Updated 12:00")
        views.setViewVisibility(R.id.widget_update_time, View.VISIBLE)
        views.setTextViewTextSize(R.id.widget_box_name, TypedValue.COMPLEX_UNIT_SP, 13f)
        views.setTextViewTextSize(R.id.widget_update_time, TypedValue.COMPLEX_UNIT_SP, 9f)
        views.setViewVisibility(R.id.widget_refresh_button, View.GONE)
        views.setViewVisibility(R.id.widget_loading_spinner, View.GONE)
        views.setViewVisibility(R.id.widget_settings_button, View.VISIBLE)
        views.setInt(R.id.widget_root, "setBackgroundColor", 0xFF0F172A.toInt())
        views.setTextColor(R.id.widget_box_name, 0xFFF8FAFC.toInt())
        views.setTextColor(R.id.widget_update_time, 0xFF94A3B8.toInt())
        views.setInt(R.id.widget_refresh_button, "setColorFilter", 0xFFF8FAFC.toInt())
        views.setInt(R.id.widget_settings_button, "setColorFilter", 0xFFF8FAFC.toInt())
        views.setInt(R.id.widget_values_container, "setBackgroundColor", 0x1AFFFFFF)

        // Rows — mirrors the list branch (6 rows)
        val titles = listOf(R.id.sensor_title_1, R.id.sensor_title_2, R.id.sensor_title_3, R.id.sensor_title_4, R.id.sensor_title_5, R.id.sensor_title_6)
        val values = listOf(R.id.sensor_value_1, R.id.sensor_value_2, R.id.sensor_value_3, R.id.sensor_value_4, R.id.sensor_value_5, R.id.sensor_value_6)
        val icons = listOf(R.id.sensor_icon_1, R.id.sensor_icon_2, R.id.sensor_icon_3, R.id.sensor_icon_4, R.id.sensor_icon_5, R.id.sensor_icon_6)
        val rows = listOf(R.id.row_1, R.id.row_2, R.id.row_3, R.id.row_4, R.id.row_5, R.id.row_6)
        for (i in 0 until 6) {
            views.setViewVisibility(rows[i], View.VISIBLE)
            views.setViewVisibility(titles[i], View.VISIBLE)
            views.setTextViewText(titles[i], "Sensor $i")
            views.setTextColor(titles[i], 0xFF94A3B8.toInt())
            views.setTextViewText(values[i], "12.3 °C")
            views.setTextViewTextSize(titles[i], TypedValue.COMPLEX_UNIT_SP, 11f)
            views.setTextViewTextSize(values[i], TypedValue.COMPLEX_UNIT_SP, 11f)
            views.setImageViewResource(icons[i], R.drawable.ic_sensor_temp)
            views.setInt(icons[i], "setColorFilter", 0xFFF97316.toInt())
            views.setTextColor(values[i], 0xFFF97316.toInt())
        }

        // Click intents — mirrors the provider's PendingIntents
        val appIntent = Intent(context, MainActivity::class.java)
        views.setOnClickPendingIntent(R.id.widget_box_name, PendingIntent.getActivity(context, 1, appIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))

        val hostRoot = views.apply(context, null)
        assertNotNull(hostRoot)
    }

    @Test
    fun `metric layout RemoteViews apply with provider action set does not throw`() {
        val views = RemoteViews(context.packageName, R.layout.widget_layout_metric)

        views.setViewVisibility(R.id.widget_header, View.VISIBLE)
        views.setTextViewText(R.id.widget_box_name, "Test Box")
        views.setViewVisibility(R.id.widget_box_name, View.VISIBLE)
        views.setTextViewText(R.id.widget_update_time, "Updated 12:00")
        views.setViewVisibility(R.id.widget_update_time, View.VISIBLE)
        views.setTextViewTextSize(R.id.widget_box_name, TypedValue.COMPLEX_UNIT_SP, 13f)
        views.setTextViewTextSize(R.id.widget_update_time, TypedValue.COMPLEX_UNIT_SP, 9f)
        views.setViewVisibility(R.id.widget_refresh_button, View.GONE)
        views.setViewVisibility(R.id.widget_loading_spinner, View.GONE)
        views.setViewVisibility(R.id.widget_settings_button, View.VISIBLE)
        views.setInt(R.id.widget_root, "setBackgroundColor", 0xFF0F172A.toInt())
        views.setInt(R.id.widget_values_container, "setBackgroundColor", 0x1AFFFFFF)

        views.setTextViewTextSize(R.id.big_sensor_value, TypedValue.COMPLEX_UNIT_SP, 26f)
        views.setTextViewTextSize(R.id.big_sensor_title, TypedValue.COMPLEX_UNIT_SP, 11f)
        views.setTextViewText(R.id.big_sensor_value, "23.4 °C")
        views.setViewVisibility(R.id.big_sensor_title, View.VISIBLE)
        views.setTextViewText(R.id.big_sensor_title, "Temperature")
        views.setImageViewResource(R.id.big_sensor_icon, R.drawable.ic_sensor_temp)
        views.setInt(R.id.big_sensor_icon, "setColorFilter", 0xFFF97316.toInt())
        views.setTextColor(R.id.big_sensor_value, 0xFFF97316.toInt())
        // API 31+ path — must be gated in production; here sdk=34 so it must still be remotable
        views.setViewLayoutWidth(R.id.big_sensor_icon, 40f, TypedValue.COMPLEX_UNIT_DIP)
        views.setViewLayoutHeight(R.id.big_sensor_icon, 40f, TypedValue.COMPLEX_UNIT_DIP)

        val hostRoot = views.apply(context, null)
        assertNotNull(hostRoot)
    }

    @Test
    fun `fallback RemoteViews is always inflatable`() {
        // Mirrors SenseBoxWidgetProvider.buildFallbackRemoteViews
        val views = RemoteViews(context.packageName, R.layout.widget_layout_list)
        views.setViewVisibility(R.id.widget_header, View.VISIBLE)
        views.setTextViewText(R.id.widget_box_name, context.getString(R.string.app_name))
        views.setViewVisibility(R.id.widget_box_name, View.VISIBLE)
        views.setViewVisibility(R.id.widget_update_time, View.GONE)
        views.setViewVisibility(R.id.widget_refresh_button, View.GONE)
        views.setViewVisibility(R.id.widget_loading_spinner, View.GONE)
        views.setViewVisibility(R.id.widget_settings_button, View.VISIBLE)
        views.setInt(R.id.widget_root, "setBackgroundColor", 0xFF0F172A.toInt())
        views.setViewVisibility(R.id.row_1, View.VISIBLE)
        views.setViewVisibility(R.id.sensor_title_1, View.GONE)
        views.setTextViewText(R.id.sensor_value_1, context.getString(R.string.widget_error_tap_reconfigure))
        views.setTextColor(R.id.sensor_value_1, 0xFFF8FAFC.toInt())
        views.setViewVisibility(R.id.row_2, View.GONE)
        views.setViewVisibility(R.id.row_3, View.GONE)
        views.setViewVisibility(R.id.row_4, View.GONE)
        views.setViewVisibility(R.id.row_5, View.GONE)
        views.setViewVisibility(R.id.row_6, View.GONE)

        val hostRoot = views.apply(context, null)
        assertNotNull(hostRoot)
    }

    @Test
    fun `widget provider holds broadcast alive with goAsync and never silently drops`() {
        // Structural guard: onUpdate / ACTION_REFRESH_WIDGET / USER_PRESENT must use goAsync(),
        // and the provider must have a fallback path. If someone removes either, this fails
        // long before a user sees "Can't load widget" again.
        val candidates = listOf(
            File("app/src/main/java/de/nichu42/boxviewer/widget/SenseBoxWidgetProvider.kt"),
            File("src/main/java/de/nichu42/boxviewer/widget/SenseBoxWidgetProvider.kt")
        )
        val source = candidates.firstOrNull { it.exists() }?.readText()
            ?: error("SenseBoxWidgetProvider.kt not found from test working dir ${File(".").absolutePath}")
        assertTrue("onUpdate must use goAsync() to keep delivery alive", source.contains("goAsync()"))
        assertTrue("provider must have a fallback RemoteViews path", source.contains("buildFallbackRemoteViews") || source.contains("applyFallbackRemoteViews"))
        assertTrue("fallback string must exist", source.contains("widget_error_tap_reconfigure"))
        // The old 0.51 bug must stay gone
        assertTrue("non-remotable spinner tint must stay removed", !source.contains("setIndeterminateTintList"))
    }
}
