package de.nichu42.boxviewer.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object ApiLogger {
    private const val FILE_NAME = "api_log.jsonl"
    private const val PREFS_NAME = "boxviewer_prefs"
    
    const val PREF_KEY_ENABLED = "pref_api_logging_enabled"
    const val PREF_KEY_MAX_ENTRIES = "pref_api_logging_max_entries"
    const val DEFAULT_MAX_ENTRIES = 100

    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var appContext: Context? = null
    private var prefs: SharedPreferences? = null

    // Holds app foreground/background state
    @Volatile
    var isAppForeground: Boolean = false

    // Cache to temporarily hold response JSON bodies mapped by URL
    val responseCache = ConcurrentHashMap<String, String>()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val entryAdapter = moshi.adapter(ApiLogEntry::class.java)

    data class ApiLogEntry(
        val timestamp: String,
        val appState: String,
        val method: String,
        val url: String,
        val status: Int,
        val durationMs: Long,
        val responseJson: String?,
        val parsingResult: String?,
        val error: String?
    )

    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isLoggingEnabled(): Boolean {
        return prefs?.getBoolean(PREF_KEY_ENABLED, false) ?: false
    }

    fun getMaxEntries(): Int {
        return prefs?.getInt(PREF_KEY_MAX_ENTRIES, DEFAULT_MAX_ENTRIES) ?: DEFAULT_MAX_ENTRIES
    }

    fun setLoggingEnabled(enabled: Boolean) {
        prefs?.edit { putBoolean(PREF_KEY_ENABLED, enabled) }
        if (!enabled) {
            // Clear cache and logs when disabling to free memory/storage
            responseCache.clear()
            clearLogs()
        }
    }

    fun setMaxEntries(maxEntries: Int) {
        prefs?.edit { putInt(PREF_KEY_MAX_ENTRIES, maxEntries) }
        // Trim logs to new size in background
        scope.launch {
            trimLogs()
        }
    }

    fun logRequest(
        method: String,
        url: String,
        status: Int,
        durationMs: Long,
        responseJson: String?,
        parsingResult: String?,
        error: String?
    ) {
        if (!isLoggingEnabled()) return

        scope.launch {
            mutex.withLock {
                val context = appContext ?: return@launch
                val file = File(context.filesDir, FILE_NAME)

                // 1. Create file and write diagnostics header if it doesn't exist
                val isNewFile = !file.exists() || file.length() == 0L
                if (isNewFile) {
                    writeDiagnosticsHeader(file)
                }

                // 2. Format request entry as a single JSON line
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                val timestamp = sdf.format(Date())
                val state = if (isAppForeground) "Foreground" else "Background"

                val entry = ApiLogEntry(
                    timestamp = timestamp,
                    appState = state,
                    method = method,
                    url = url,
                    status = status,
                    durationMs = durationMs,
                    responseJson = responseJson,
                    parsingResult = parsingResult,
                    error = error
                )

                try {
                    val jsonLine = entryAdapter.toJson(entry)
                    file.appendText(jsonLine + "\n")
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 3. Trim logs if limit exceeded
                trimLogsLocked(file)
            }
        }
    }

    private fun writeDiagnosticsHeader(file: File) {
        try {
            val context = appContext ?: return
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

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            
            // Format system diagnostics as the first JSON line
            val header = mapOf(
                "type" to "diagnostics",
                "date" to sdf.format(Date()),
                "appVersion" to "$versionName ($versionCode)",
                "androidSdk" to Build.VERSION.SDK_INT,
                "androidOs" to Build.VERSION.RELEASE,
                "device" to "${Build.MANUFACTURER} ${Build.MODEL}",
                "brand" to Build.BRAND,
                "hardware" to Build.HARDWARE,
                "status" to "Running"
            )
            
            val headerJson = moshi.adapter(Map::class.java).toJson(header)
            file.writeText(headerJson + "\n")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun trimLogs() {
        mutex.withLock {
            val context = appContext ?: return
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists()) {
                trimLogsLocked(file)
            }
        }
    }

    private fun trimLogsLocked(file: File) {
        try {
            val maxEntries = getMaxEntries()
            val lines = file.readLines()
            if (lines.size > maxEntries + 1) { // +1 for the header line
                val header = lines.firstOrNull() ?: ""
                val logsToKeep = lines.drop(1).takeLast(maxEntries)
                
                // Rewrite file
                file.writeText(header + "\n")
                for (log in logsToKeep) {
                    file.appendText(log + "\n")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearLogs() {
        scope.launch {
            mutex.withLock {
                val context = appContext ?: return@launch
                val file = File(context.filesDir, FILE_NAME)
                if (file.exists()) {
                    file.delete()
                }
            }
        }
    }

    suspend fun getLogsText(): String {
        return mutex.withLock {
            val context = appContext ?: return@withLock ""
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists()) {
                try {
                    file.readText()
                } catch (e: Exception) {
                    ""
                }
            } else {
                ""
            }
        }
    }

    suspend fun parseLogs(): Pair<Map<String, Any>?, List<ApiLogEntry>> {
        return mutex.withLock {
            val context = appContext ?: return@withLock Pair(null, emptyList())
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) return@withLock Pair(null, emptyList())

            val diagnostics = mutableMapOf<String, Any>()
            val entries = mutableListOf<ApiLogEntry>()

            try {
                file.forEachLine { line ->
                    if (line.isBlank()) return@forEachLine
                    if (line.contains("\"type\":\"diagnostics\"") || line.contains("\"type\" : \"diagnostics\"")) {
                        try {
                            @Suppress("UNCHECKED_CAST")
                            val map = moshi.adapter(Map::class.java).fromJson(line) as? Map<String, Any>
                            if (map != null) {
                                diagnostics.putAll(map)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        try {
                            val entry = entryAdapter.fromJson(line)
                            if (entry != null) {
                                entries.add(entry)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Pair(diagnostics.ifEmpty { null }, entries.reversed())
        }
    }

    suspend fun getLogFileSize(): Long {
        return mutex.withLock {
            val context = appContext ?: return@withLock 0L
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists()) file.length() else 0L
        }
    }
}
