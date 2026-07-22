package de.nichu42.boxviewer.util

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import de.nichu42.boxviewer.data.db.DB_VERSION
import de.nichu42.boxviewer.widget.SenseBoxWidgetProvider
import de.nichu42.boxviewer.widget.SenseBoxWidgetProviderLarge
import de.nichu42.boxviewer.widget.SenseBoxWidgetProviderSmall
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashHandler {
    private const val FILE_NAME = "crash_log.txt"

    fun init(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            saveCrashLog(context, throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun saveCrashLog(context: Context, throwable: Throwable) {
        try {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            val stackTrace = sw.toString()

            val pm = context.packageManager
            val pi = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(context.packageName, 0)
                }
            } catch (e: Exception) {
                null
            }

            val versionName = pi?.versionName ?: "Unknown"
            val versionCode = pi?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode else @Suppress("DEPRECATION") it.versionCode
            } ?: 0

            val log = StringBuilder().apply {
                append("=== BOXVIEWER CRASH REPORT ===\n")
                append("Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
                append("App Version: $versionName ($versionCode)\n")
                append("Android SDK: ${Build.VERSION.SDK_INT}\n")
                append("Android OS: ${Build.VERSION.RELEASE}\n")
                append("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
                append("Brand: ${Build.BRAND}\n")
                append("Hardware: ${Build.HARDWARE}\n")
                append("Thread: ${Thread.currentThread().name}\n")
                append("\n--- Stack Trace ---\n")
                append(stackTrace)
            }.toString()

            val file = File(context.filesDir, FILE_NAME)
            file.writeText(log)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCrashLog(context: Context): String? {
        val file = File(context.filesDir, FILE_NAME)
        return if (file.exists()) {
            try {
                file.readText()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    fun clearCrashLog(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        if (file.exists()) {
            file.delete()
        }
    }

    fun generateSystemDiagnostics(context: Context): String {
        val pm = context.packageManager
        val pi = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, 0)
            }
        } catch (e: Exception) {
            null
        }
        val versionName = pi?.versionName ?: "Unknown"
        val versionCode = pi?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode else @Suppress("DEPRECATION") it.versionCode
        } ?: 0

        val crashLog = getCrashLog(context)
        val statusText = if (crashLog != null) "Crash Recorded (See log below)" else "Running Normally"

        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val savedLanguage = prefs.getString("app_language", "system_default")
        val activeLocale = Locale.getDefault().toLanguageTag()
        val tempUnit = prefs.getString("temperature_unit", "°C")
        val pressUnit = prefs.getString("pressure_unit", "hPa")
        val formatPressure = prefs.getBoolean("format_pressure", true)
        val windUnit = prefs.getString("wind_unit", "m/s")
        val aqiSystem = prefs.getString("aqi_system", AqiSystem.US_EPA.name) ?: AqiSystem.US_EPA.name
        val textScale = prefs.getFloat("app_text_scale", 1.0f)
        val appTheme = prefs.getString("app_theme", "SYSTEM") ?: "SYSTEM"

        val appWidgetManager = try { AppWidgetManager.getInstance(context) } catch (e: Exception) { null }
        val mediumWidgets = appWidgetManager?.getAppWidgetIds(ComponentName(context, SenseBoxWidgetProvider::class.java))?.size ?: 0
        val smallWidgets = appWidgetManager?.getAppWidgetIds(ComponentName(context, SenseBoxWidgetProviderSmall::class.java))?.size ?: 0
        val largeWidgets = appWidgetManager?.getAppWidgetIds(ComponentName(context, SenseBoxWidgetProviderLarge::class.java))?.size ?: 0
        val totalWidgets = mediumWidgets + smallWidgets + largeWidgets

        val dbFile = context.getDatabasePath("sensebox_database")
        val dbExist = dbFile.exists()
        val dbSizeKb = if (dbExist) dbFile.length() / 1024 else 0L

        val apiLogEnabled = ApiLogger.isLoggingEnabled()

        return StringBuilder().apply {
            append("=== BOXVIEWER SYSTEM DIAGNOSTICS ===\n")
            append("Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
            append("App Version: $versionName ($versionCode)\n")
            append("Android SDK: ${Build.VERSION.SDK_INT}\n")
            append("Android OS: ${Build.VERSION.RELEASE}\n")
            append("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
            append("Brand: ${Build.BRAND}\n")
            append("Hardware: ${Build.HARDWARE}\n")
            append("Status: $statusText\n")
            append("\n--- Home Widgets ---\n")
            append("Active Widgets: $totalWidgets (Medium: $mediumWidgets, Small: $smallWidgets, Large: $largeWidgets)\n")
            append("\n--- Preferences & State ---\n")
            append("App Language: $savedLanguage (System Locale: $activeLocale)\n")
            append("Temperature Unit: $tempUnit\n")
            append("Pressure Unit: $pressUnit (Format: $formatPressure)\n")
            append("Wind Unit: $windUnit\n")
            append("AQI Standard: $aqiSystem\n")
            append("Text Scale: ${String.format(Locale.US, "%.1fx", textScale)}\n")
            append("Theme: $appTheme\n")
            append("\n--- Database & Diagnostics ---\n")
            append("Database: ${if (dbExist) "Present (${dbSizeKb} KB, Schema v$DB_VERSION)" else "Missing"}\n")
            append("API Logging Enabled: $apiLogEnabled\n")

            if (crashLog != null) {
                append("\n--- Saved Crash Report ---\n")
                append(crashLog)
            }
        }.toString()
    }
}
