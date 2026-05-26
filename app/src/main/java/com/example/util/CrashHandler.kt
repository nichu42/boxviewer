package com.example.util

import android.content.Context
import android.os.Build
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

        return StringBuilder().apply {
            append("=== BOXVIEWER SYSTEM DIAGNOSTICS ===\n")
            append("Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
            append("App Version: $versionName ($versionCode)\n")
            append("Android SDK: ${Build.VERSION.SDK_INT}\n")
            append("Android OS: ${Build.VERSION.RELEASE}\n")
            append("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
            append("Brand: ${Build.BRAND}\n")
            append("Hardware: ${Build.HARDWARE}\n")
            append("Status: Running Normally\n")
        }.toString()
    }
}
