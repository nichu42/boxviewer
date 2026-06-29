package de.nichu42.boxviewer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.nichu42.boxviewer.data.api.SenseBox
import de.nichu42.boxviewer.data.db.SenseBoxDatabase
import de.nichu42.boxviewer.data.db.SensorCacheEntity
import de.nichu42.boxviewer.data.repository.SenseBoxRepository
import de.nichu42.boxviewer.util.AqiSystem
import de.nichu42.boxviewer.util.AqiCalculator
import de.nichu42.boxviewer.util.AqiResult
import de.nichu42.boxviewer.util.SensorSortKey
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import androidx.core.content.edit
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import de.nichu42.boxviewer.widget.SenseBoxWidgetProvider

class SenseBoxViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SenseBoxDatabase.getDatabase(application)
    private val repository = SenseBoxRepository(application, db)

    val savedBoxes: StateFlow<List<de.nichu42.boxviewer.data.db.SavedBoxEntity>> = repository.savedBoxes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    enum class AppTheme(val label: String) {
        SYSTEM("System"),
        LIGHT("Light"),
        DARK("Dark")
    }

    private val _useConditionalFormatting = MutableStateFlow(true)
    val useConditionalFormatting: StateFlow<Boolean> = _useConditionalFormatting.asStateFlow()

    private val _temperatureUnit = MutableStateFlow("°C")
    val temperatureUnit: StateFlow<String> = _temperatureUnit.asStateFlow()

    private val _pressureUnit = MutableStateFlow("hPa")
    val pressureUnit: StateFlow<String> = _pressureUnit.asStateFlow()

    private val _windUnit = MutableStateFlow("m/s")
    val windUnit: StateFlow<String> = _windUnit.asStateFlow()

    private val _formatPressure = MutableStateFlow(true)
    val formatPressure: StateFlow<Boolean> = _formatPressure.asStateFlow()

    private val _appTheme = MutableStateFlow(AppTheme.SYSTEM)
    val appTheme: StateFlow<AppTheme> = _appTheme.asStateFlow()

    private val _aqiSystem = MutableStateFlow(AqiSystem.US_EPA)
    val aqiSystem: StateFlow<AqiSystem> = _aqiSystem.asStateFlow()

    private val _lastUpdatedMinutes = MutableStateFlow(60) // Default 60 minutes (1 hour)
    val lastUpdatedMinutes: StateFlow<Int> = _lastUpdatedMinutes.asStateFlow()

    fun setLastUpdatedMinutes(minutes: Int) {
        _lastUpdatedMinutes.value = minutes
    }

    fun toggleSensorExpanded(sensorId: String) {
        val current = _expandedSensorIds.value
        _expandedSensorIds.value = if (sensorId in current) current - sensorId else current + sensorId
    }

    fun loadSensorHistoryIfNeeded(boxId: String, sensorId: String, limit: Int) {
        val key = "$boxId/$sensorId"
        if (_sensorHistoryCache.value.containsKey(key)) return
        if (_sensorHistoryLoading.value.contains(key)) return
        viewModelScope.launch {
            _sensorHistoryLoading.value += key
            try {
                val measurements = getSensorData(boxId, sensorId, limit).reversed()
                _sensorHistoryCache.value += (key to measurements)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _sensorHistoryLoading.value -= key
            }
        }
    }

    fun clearDetailScreenCache() {
        _sensorHistoryCache.value = emptyMap()
        _sensorHistoryLoading.value = emptySet()
        _expandedSensorIds.value = emptySet()
    }

    data class OpenSenseMapStats(
        val boxesCount: String,
        val measurementsCount: String,
        val measurementsLastMinute: String
    )

    private val _globalStats = MutableStateFlow<OpenSenseMapStats?>(null)
    val globalStats: StateFlow<OpenSenseMapStats?> = _globalStats.asStateFlow()

    private val _isLoadingStats = MutableStateFlow(false)
    val isLoadingStats: StateFlow<Boolean> = _isLoadingStats.asStateFlow()

    private val _statsError = MutableStateFlow<String?>(null)
    val statsError: StateFlow<String?> = _statsError.asStateFlow()

    fun fetchGlobalStats() {
        viewModelScope.launch {
            _isLoadingStats.value = true
            _statsError.value = null
            try {
                val response = repository.getStats(human = true)
                if (response.size >= 3) {
                    _globalStats.value = OpenSenseMapStats(
                        boxesCount = response[0],
                        measurementsCount = response[1],
                        measurementsLastMinute = response[2]
                    )
                } else {
                    _statsError.value = "Invalid stats format returned by API."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _statsError.value = "Failed to load database stats: ${e.localizedMessage ?: e.message}"
            } finally {
                _isLoadingStats.value = false
            }
        }
    }

    init {
        val prefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        _useConditionalFormatting.value = prefs.getBoolean("use_conditional_formatting", true)

        val oldFahrenheit = prefs.getBoolean("use_fahrenheit", false)
        val defaultTemp = if (oldFahrenheit) "°F" else "°C"
        _temperatureUnit.value = prefs.getString("temperature_unit", defaultTemp) ?: defaultTemp
        _pressureUnit.value = prefs.getString("pressure_unit", "hPa") ?: "hPa"
        _windUnit.value = prefs.getString("wind_unit", "m/s") ?: "m/s"

        _formatPressure.value = prefs.getBoolean("format_pressure", true)
        val themeStr = prefs.getString("app_theme", AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
        _appTheme.value = try { AppTheme.valueOf(themeStr) } catch (e: Exception) { AppTheme.SYSTEM }

        val aqiStr = prefs.getString("aqi_system", AqiSystem.US_EPA.name) ?: AqiSystem.US_EPA.name
        _aqiSystem.value = try { AqiSystem.valueOf(aqiStr) } catch(e: Exception) { AqiSystem.US_EPA }

        // No automatic nearby search on startup, but sync saved boxes to make sure their values are up to date!
        viewModelScope.launch {
            try {
                repository.refreshAllSavedBoxes()
                SenseBoxWidgetProvider.updateAllWidgetsFromCache(getApplication())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setUseConditionalFormatting(use: Boolean) {
        _useConditionalFormatting.value = use
        getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE).edit {
            putBoolean("use_conditional_formatting", use)
        }
        SenseBoxWidgetProvider.updateAllWidgetsFromCache(getApplication())
    }

    fun setTemperatureUnit(unit: String) {
        _temperatureUnit.value = unit
        getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE).edit {
            putString("temperature_unit", unit)
        }
        SenseBoxWidgetProvider.updateAllWidgetsFromCache(getApplication())
    }

    fun setPressureUnit(unit: String) {
        _pressureUnit.value = unit
        getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE).edit {
            putString("pressure_unit", unit)
        }
        SenseBoxWidgetProvider.updateAllWidgetsFromCache(getApplication())
    }

    fun setWindUnit(unit: String) {
        _windUnit.value = unit
        getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE).edit {
            putString("wind_unit", unit)
        }
        SenseBoxWidgetProvider.updateAllWidgetsFromCache(getApplication())
    }

    fun setFormatPressure(format: Boolean) {
        _formatPressure.value = format
        getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE).edit {
            putBoolean("format_pressure", format)
        }
        SenseBoxWidgetProvider.updateAllWidgetsFromCache(getApplication())
    }

    fun setAppTheme(theme: AppTheme) {
        _appTheme.value = theme
        getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE).edit {
            putString("app_theme", theme.name)
        }
    }

    fun setAqiSystem(system: AqiSystem) {
        _aqiSystem.value = system
        getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE).edit {
            putString("aqi_system", system.name)
        }
        SenseBoxWidgetProvider.updateAllWidgetsFromCache(getApplication())
    }

    enum class ExposureFilter(val label: String) {
        OUTDOOR("Outdoor"),
        INDOOR("Indoor"),
        ALL("All")
    }

    private val _rawDiscoveredBoxes = MutableStateFlow<List<SenseBox>>(emptyList())
    val rawDiscoveredBoxes: StateFlow<List<SenseBox>> = _rawDiscoveredBoxes.asStateFlow()
    
    val selectedExposure = MutableStateFlow(ExposureFilter.OUTDOOR)
    


    // Reverse-geocoded locations cache mapping boxId -> "City, State, Country"
    private val _boxLocations = MutableStateFlow<Map<String, String>>(emptyMap())
    val boxLocations: StateFlow<Map<String, String>> = _boxLocations.asStateFlow()

    private val _searchRadiusKm = MutableStateFlow(25)
    val searchRadiusKm: StateFlow<Int> = _searchRadiusKm.asStateFlow()

    fun setSearchRadiusKm(km: Int) {
        _searchRadiusKm.value = km
    }

    private val boxLastUpdatedCache = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val boxLastUpdatedTextCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val boxAddressCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val boxFullAddressCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    // Per-box sensor history cache: key = "$boxId/$sensorId" -> measurements list
    // Survives LazyColumn item disposal and back-and-forth navigation within the same box.
    private val _sensorHistoryCache = MutableStateFlow<Map<String, List<de.nichu42.boxviewer.data.api.Measurement>>>(emptyMap())
    val sensorHistoryCache: StateFlow<Map<String, List<de.nichu42.boxviewer.data.api.Measurement>>> = _sensorHistoryCache.asStateFlow()

    private val _sensorHistoryLoading = MutableStateFlow<Set<String>>(emptySet())
    val sensorHistoryLoading: StateFlow<Set<String>> = _sensorHistoryLoading.asStateFlow()

    // Track which sensor cards are expanded
    private val _expandedSensorIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedSensorIds: StateFlow<Set<String>> = _expandedSensorIds.asStateFlow()

    private val _isLocationSearch = MutableStateFlow(true)
    val isLocationSearch: StateFlow<Boolean> = _isLocationSearch.asStateFlow()

    fun clearSearch() {
        _rawDiscoveredBoxes.value = emptyList()
        _hasSearchBeenDone.value = false
        _errorMessage.value = null
        _searchRadiusUsed.value = null
        lastSearchedCoords.value = null
        _autocompleteResults.value = emptyList()
    }

    fun setIsLocationSearch(isLocation: Boolean) {
        _isLocationSearch.value = isLocation
        clearSearch()
    }

    val discoveredBoxes: StateFlow<List<SenseBox>> = combine(
        _rawDiscoveredBoxes,
        selectedExposure,
        _lastUpdatedMinutes,
        _searchRadiusKm,
        _isLocationSearch
    ) { rawList, exposure, lastMinutes, radiusKm, isLocSearch ->
        val filtered = if (!isLocSearch) {
            rawList
        } else {
            filterBoxes(rawList, exposure, lastMinutes, radiusKm)
        }
        filtered.distinctBy { it.id }
    }
    .flowOn(kotlinx.coroutines.Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _searchRadiusUsed = MutableStateFlow<Int?>(null)
    val searchRadiusUsed: StateFlow<Int?> = _searchRadiusUsed.asStateFlow()

    val lastSearchedCoords = MutableStateFlow<Pair<Double, Double>?>(null)

    private val _autocompleteResults = MutableStateFlow<List<android.location.Address>>(emptyList())
    val autocompleteResults: StateFlow<List<android.location.Address>> = _autocompleteResults.asStateFlow()

    private var autocompleteJob: kotlinx.coroutines.Job? = null

    fun onLocationQueryChanged(query: String) {
        autocompleteJob?.cancel()
        if (query.trim().length < 3) {
            _autocompleteResults.value = emptyList()
            return
        }
        autocompleteJob = viewModelScope.launch {
            kotlinx.coroutines.delay(400.milliseconds) // Debounce typing
            try {
                // 1. Native Geocoder query (run in background IO dispatcher)
                val geocoder = android.location.Geocoder(getApplication(), java.util.Locale.getDefault())
                val nativeAddresses = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocationName(query, 5)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }

                // 2. Photon (EU-based) OSM geocoder as the primary fallback, then Nominatim.
                val photonAddresses = photonForwardGeocode(query)
                val osmAddresses = photonAddresses.ifEmpty { nominatimForwardGeocode(query) }

                // 3. Combine all lists, filter duplicates by label, and take up to 3-5 results
                val combined = (nativeAddresses ?: emptyList()) + osmAddresses
                val distinct = combined.distinctBy { addr ->
                    val line = addr.getAddressLine(0) ?: addr.locality ?: ""
                    line.lowercase().trim()
                }.take(5)

                _autocompleteResults.value = distinct
            } catch (e: Exception) {
                e.printStackTrace()
                _autocompleteResults.value = emptyList()
            }
        }
    }

    fun clearAutocomplete() {
        _autocompleteResults.value = emptyList()
    }

    fun searchByAddress(address: android.location.Address) {
        val lat = address.latitude
        val lng = address.longitude
        val cityName = address.locality ?: address.subAdminArea ?: address.subLocality ?: address.getAddressLine(0) ?: "Selected Location"
        lastSearchedCoords.value = Pair(lng, lat)
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _searchRadiusUsed.value = null
            _autocompleteResults.value = emptyList()
            _hasSearchBeenDone.value = true
            try {
                var found = false
                val steps = listOf(25000, 50000, 100000, 250000, 500000)
                for (radius in steps) {
                    val results = repository.findNearBoxes(lng, lat, radius)
                    if (results.isNotEmpty()) {
                        _rawDiscoveredBoxes.value = results
                        resolveLocationsFor(results)
                        _searchRadiusUsed.value = radius
                        _searchRadiusKm.value = radius / 1000
                        found = true
                        break
                    }
                }
                if (!found) {
                    _rawDiscoveredBoxes.value = emptyList()
                    _errorMessage.value = "No senseBoxes found within 500 km of \"$cityName\"."
                    _searchRadiusUsed.value = 500000
                    _searchRadiusKm.value = 500
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Failed to search location: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private val _hasSearchBeenDone = MutableStateFlow(false)
    val hasSearchBeenDone: StateFlow<Boolean> = _hasSearchBeenDone.asStateFlow()

    private val _selectedBox = MutableStateFlow<SenseBox?>(null)
    val selectedBox: StateFlow<SenseBox?> = _selectedBox.asStateFlow()

    private val _cachedSensors = MutableStateFlow<List<SensorCacheEntity>>(emptyList())
    val cachedSensors: StateFlow<List<SensorCacheEntity>> = _cachedSensors.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _autoConfigureBoxId = MutableStateFlow<String?>(null)
    val autoConfigureBoxId: StateFlow<String?> = _autoConfigureBoxId.asStateFlow()

    fun setErrorMessage(msg: String?) {
        _errorMessage.value = msg
    }

    fun setAutoConfigureBox(boxId: String?) {
        _autoConfigureBoxId.value = boxId
    }

    private val _boxPreview = MutableStateFlow<SenseBox?>(null)
    val boxPreview: StateFlow<SenseBox?> = _boxPreview.asStateFlow()

    private val _isPreviewLoading = MutableStateFlow(false)
    val isPreviewLoading: StateFlow<Boolean> = _isPreviewLoading.asStateFlow()

    private val _previewError = MutableStateFlow<String?>(null)
    val previewError: StateFlow<String?> = _previewError.asStateFlow()

    private val _previewLocation = MutableStateFlow<String?>(null)
    val previewLocation: StateFlow<String?> = _previewLocation.asStateFlow()

    fun loadBoxPreview(boxId: String) {
        viewModelScope.launch {
            _isPreviewLoading.value = true
            _previewError.value = null
            _boxPreview.value = null
            _previewLocation.value = null
            try {
                val box = repository.fetchBoxPreview(boxId)
                _boxPreview.value = box
                val coords = box.currentLocation
                if (coords != null) {
                    getCityStateCountryFromLocation(box.id, coords.latitude, coords.longitude) { label ->
                        _previewLocation.value = label
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _previewError.value = "Couldn't load this senseBox. Check the link or your connection."
            } finally {
                _isPreviewLoading.value = false
            }
        }
    }

    fun clearBoxPreview() {
        _boxPreview.value = null
        _isPreviewLoading.value = false
        _previewError.value = null
        _previewLocation.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun parseObjectIdTimestamp(id: String): java.util.Date? {
        if (id.length != 24) return null
        return try {
            val hexTime = id.substring(0, 8)
            val epochSeconds = hexTime.toLong(16)
            java.util.Date(epochSeconds * 1000L)
        } catch (_: Exception) {
            null
        }
    }

    fun getSensorLastMeasurementDate(sensor: de.nichu42.boxviewer.data.api.Sensor): java.util.Date? {
        val m = sensor.lastMeasurement ?: return null
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        m.createdAt?.let { ts ->
            if (ts.length >= 19) {
                for (format in formats) {
                    try {
                        val sdf = java.text.SimpleDateFormat(format, java.util.Locale.US).apply {
                            timeZone = java.util.TimeZone.getTimeZone("UTC")
                            isLenient = true
                        }
                        val parsedDate = sdf.parse(ts)
                        if (parsedDate != null) return parsedDate
                    } catch (_: Exception) {}
                }
            }
        }
        return m.value?.let { v ->
            parseObjectIdTimestamp(v)
        }
    }

    fun hasOutdatedSensors(box: SenseBox, thresholdMinutes: Int): Boolean {
        if (thresholdMinutes >= 999999) return false
        val sensors = box.sensors ?: return false
        if (sensors.isEmpty()) return false
        
        val now = java.util.Date()
        val maxAgeMs = thresholdMinutes * 60 * 1000L
        
        var hasActive = false
        var hasInactive = false
        
        for (sensor in sensors) {
            val lastDate = getSensorLastMeasurementDate(sensor)
            if (lastDate == null) {
                hasInactive = true
            } else {
                val ageMs = now.time - lastDate.time
                if (ageMs <= maxAgeMs) {
                    hasActive = true
                } else {
                    hasInactive = true
                }
            }
        }
        
        return hasActive && hasInactive
    }

    fun getBoxLastUpdatedDate(box: SenseBox): java.util.Date? {
        val cached = boxLastUpdatedCache[box.id]
        if (cached != null) {
            return if (cached == -1L) null else java.util.Date(cached)
        }

        val sensorsList = box.sensors
        if (sensorsList == null) {
            boxLastUpdatedCache[box.id] = -1L
            return null
        }
        val dates = sensorsList.mapNotNull { getSensorLastMeasurementDate(it) }
        val maxDate = dates.maxOrNull()
        if (maxDate != null) {
            boxLastUpdatedCache[box.id] = maxDate.time
        } else {
            boxLastUpdatedCache[box.id] = -1L
        }
        return maxDate
    }

    fun formatLastUpdated(box: SenseBox): String {
        val cached = boxLastUpdatedTextCache[box.id]
        if (cached != null) return cached

        val date = getBoxLastUpdatedDate(box) ?: return "Never updated"
        val result = try {
            val sdf = java.text.SimpleDateFormat("MMM d, yyyy - HH:mm", java.util.Locale.getDefault())
            "Last updated: ${sdf.format(date)}"
        } catch (_: Exception) {
            "Never updated"
        }
        boxLastUpdatedTextCache[box.id] = result
        return result
    }

    fun formatMeasurementTime(sensors: List<SensorCacheEntity>): String {
        if (sensors.isEmpty()) return ""
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        val dates = sensors.mapNotNull { sensor ->
            val ts = sensor.updatedAt ?: return@mapNotNull null
            if (ts.length >= 19) {
                for (format in formats) {
                    try {
                        val sdf = java.text.SimpleDateFormat(format, java.util.Locale.US).apply {
                            timeZone = java.util.TimeZone.getTimeZone("UTC")
                            isLenient = true
                        }
                        val parsedDate = sdf.parse(ts)
                        if (parsedDate != null) return@mapNotNull parsedDate
                    } catch (_: Exception) {
                    }
                }
            }
            parseObjectIdTimestamp(ts)
        }
        val maxDate = dates.maxOrNull() ?: return ""
        return try {
            val sdf = java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.getDefault())
            sdf.format(maxDate)
        } catch (_: Exception) {
            ""
        }
    }

    fun formatAppSyncTime(sensors: List<SensorCacheEntity>): String {
        if (sensors.isEmpty()) return "Never"
        val maxFetchedAt = sensors.maxOfOrNull { it.localFetchedAt } ?: return "Never"
        if (maxFetchedAt == 0L) return "Never"

        return try {
            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            sdf.format(java.util.Date(maxFetchedAt))
        } catch (_: Exception) {
            "Never"
        }
    }

    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        fun toRad(deg: Double) = deg * kotlin.math.PI / 180.0
        val dLat = toRad(lat2 - lat1)
        val dLon = toRad(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(toRad(lat1)) * kotlin.math.cos(toRad(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return r * c
    }

    /**
     * Forward-geocode a query using Photon (EU-based OpenStreetMap geocoder by komoot).
     * Returns up to 5 Address objects.
     */
    private suspend fun photonForwardGeocode(query: String): List<android.location.Address> {
        return withContext(kotlinx.coroutines.Dispatchers.IO) {
            val list = mutableListOf<android.location.Address>()
            try {
                val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                val url = "https://photon.komoot.io/api/?q=$encodedQuery&limit=5&lang=en"
                val client = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .header("User-Agent", "BoxViewer/${de.nichu42.boxviewer.BuildConfig.VERSION_NAME} (contact: nichu42@42bit.email)")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body.string()
                        if (body.isNotBlank()) {
                            val json = org.json.JSONObject(body)
                            val features = json.optJSONArray("features") ?: return@withContext list
                            for (i in 0 until features.length()) {
                                val feature = features.getJSONObject(i)
                                val geometry = feature.optJSONObject("geometry")
                                val coords = geometry?.optJSONArray("coordinates")
                                val lon = coords?.optDouble(0, 0.0) ?: 0.0
                                val lat = coords?.optDouble(1, 0.0) ?: 0.0
                                val props = feature.optJSONObject("properties") ?: continue
                                val name = props.optString("name", "").trim()
                                val street = props.optString("street", "").trim()
                                val housenumber = props.optString("housenumber", "").trim()
                                val postcode = props.optString("postcode", "").trim()
                                val city = props.optString("city", "").trim()
                                val state = props.optString("state", "").trim()
                                val country = props.optString("country", "").trim()
                                val cc = props.optString("countrycode", "").trim().uppercase(java.util.Locale.US)

                                val streetLine = listOf(street, housenumber).filter { it.isNotBlank() }.joinToString(" ")
                                val cityLine = listOf(postcode, city).filter { it.isNotBlank() }.joinToString(" ")
                                val displayName = listOfNotNull(
                                    name.takeIf { it.isNotBlank() },
                                    streetLine.takeIf { it.isNotBlank() },
                                    cityLine.takeIf { it.isNotBlank() },
                                    state.takeIf { it.isNotBlank() },
                                    country.takeIf { it.isNotBlank() }
                                ).filter { it.isNotBlank() }.joinToString(", ")

                                if (displayName.isNotBlank() && lat != 0.0 && lon != 0.0) {
                                    list.add(android.location.Address(java.util.Locale.US).apply {
                                        latitude = lat
                                        longitude = lon
                                        setAddressLine(0, displayName)
                                        locality = city.takeIf { it.isNotBlank() } ?: name
                                        adminArea = state
                                        countryName = country
                                        countryCode = cc
                                    })
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            list
        }
    }

    /**
     * Forward-geocode a query using Nominatim as a fallback.
     * Returns up to 5 Address objects.
     */
    private suspend fun nominatimForwardGeocode(query: String): List<android.location.Address> {
        return withContext(kotlinx.coroutines.Dispatchers.IO) {
            val list = mutableListOf<android.location.Address>()
            try {
                val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                val url = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=5&accept-language=en"
                val client = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .header("User-Agent", "BoxViewer/${de.nichu42.boxviewer.BuildConfig.VERSION_NAME} (contact: nichu42@42bit.email)")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body.string()
                        if (body.isNotBlank()) {
                            val jsonArray = org.json.JSONArray(body)
                            for (i in 0 until jsonArray.length()) {
                                val obj = jsonArray.getJSONObject(i)
                                val lat = obj.optDouble("lat", 0.0)
                                val lon = obj.optDouble("lon", 0.0)
                                val displayName = obj.optString("display_name", "")

                                if (displayName.isNotBlank()) {
                                    list.add(android.location.Address(java.util.Locale.US).apply {
                                        latitude = lat
                                        longitude = lon
                                        setAddressLine(0, displayName)
                                        val name = obj.optString("name", "")
                                        if (name.isNotBlank()) {
                                            locality = name
                                        }
                                    })
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            list
        }
    }

    /**
     * Reverse-geocode a coordinate. Tries the Android native Geocoder first, then falls back
     * to Photon (EU-based), then to Nominatim. De-Googled devices often have no cloud-backed
     * Geocoder, so a fallback is required for a usable location label.
     */
    private suspend fun reverseGeocodeWithFallback(
        lat: Double,
        lng: Double,
        fullAddress: Boolean = false
    ): String {
        // 1. Try native Android Geocoder reverse lookup
        val nativeAddress = withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val geocoder = android.location.Geocoder(getApplication(), java.util.Locale.getDefault())
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        if (nativeAddress != null) {
            val city = (nativeAddress.locality ?: nativeAddress.subAdminArea ?: nativeAddress.subLocality ?: "").trim()
            val state = (nativeAddress.adminArea ?: "").trim()
            val country = if (fullAddress) {
                (nativeAddress.countryName ?: "").trim()
            } else {
                (nativeAddress.countryCode ?: "").trim().uppercase(java.util.Locale.US)
            }
            val formattedState = if (fullAddress) state else abbreviateState(state)
            val parts = listOf(city, formattedState, country).filter { it.isNotBlank() }
            if (parts.isNotEmpty()) {
                return parts.joinToString(", ")
            }
        }

        // 2. Fallback to Photon (EU-based), then Nominatim
        val photonLabel = photonReverseGeocode(lat, lng, fullAddress)
        if (photonLabel.isNotBlank()) return photonLabel

        return nominatimReverseGeocode(lat, lng, fullAddress)
    }

    /**
     * Reverse-geocode a coordinate using Photon (EU-based OpenStreetMap geocoder by komoot).
     */
    private suspend fun photonReverseGeocode(lat: Double, lng: Double, fullAddress: Boolean): String {
        return withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = "https://photon.komoot.io/reverse?lon=$lng&lat=$lat&lang=en"
                val client = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .header("User-Agent", "BoxViewer/${de.nichu42.boxviewer.BuildConfig.VERSION_NAME} (contact: nichu42@42bit.email)")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body.string()
                        if (body.isNotBlank()) {
                            val json = org.json.JSONObject(body)
                            val features = json.optJSONArray("features")
                            if (features != null && features.length() > 0) {
                                val props = features.getJSONObject(0).optJSONObject("properties")
                                if (props != null) {
                                    val city = props.optString("city", "").ifBlank {
                                        props.optString("town", "").ifBlank {
                                            props.optString("village", "").ifBlank {
                                                props.optString("suburb", "").ifBlank {
                                                    props.optString("county", "").ifBlank {
                                                        props.optString("municipality", "")
                                                    }
                                                }
                                            }
                                        }
                                    }.trim()
                                    val state = props.optString("state", "").trim()
                                    val country = if (fullAddress) {
                                        props.optString("country", "").trim()
                                    } else {
                                        props.optString("countrycode", "").trim().uppercase(java.util.Locale.US)
                                    }
                                    val formattedState = if (fullAddress) state else abbreviateState(state)
                                    val parts = listOf(city, formattedState, country).filter { it.isNotBlank() }
                                    if (parts.isNotEmpty()) {
                                        return@withContext parts.joinToString(", ")
                                    }
                                }
                            }
                        }
                    }
                    ""
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        }
    }

    /**
     * Reverse-geocode a coordinate using Nominatim as a fallback.
     */
    private suspend fun nominatimReverseGeocode(lat: Double, lng: Double, fullAddress: Boolean): String {
        return withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = "https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lng&format=json&accept-language=en"
                val client = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .header("User-Agent", "BoxViewer/${de.nichu42.boxviewer.BuildConfig.VERSION_NAME} (contact: nichu42@42bit.email)")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body.string()
                        if (body.isNotBlank()) {
                            val json = org.json.JSONObject(body)
                            val addressObj = json.optJSONObject("address")
                            if (addressObj != null) {
                                val city = addressObj.optString("city", "").ifBlank {
                                    addressObj.optString("town", "").ifBlank {
                                        addressObj.optString("village", "").ifBlank {
                                            addressObj.optString("suburb", "").ifBlank {
                                                addressObj.optString("county", "").ifBlank {
                                                    addressObj.optString("municipality", "")
                                                }
                                            }
                                        }
                                    }
                                }.trim()
                                val state = addressObj.optString("state", "").trim()
                                val country = if (fullAddress) {
                                    addressObj.optString("country", "").trim()
                                } else {
                                    addressObj.optString("country_code", "").trim().uppercase(java.util.Locale.US)
                                }
                                val formattedState = if (fullAddress) state else abbreviateState(state)
                                val parts = listOf(city, formattedState, country).filter { it.isNotBlank() }
                                if (parts.isNotEmpty()) {
                                    return@withContext parts.joinToString(", ")
                                }
                            }
                        }
                    }
                    ""
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        }
    }

    fun getAddressFromLocation(lat: Double, lng: Double, onResult: (String) -> Unit) {
        viewModelScope.launch {
            var label = "Location (${"%.4f".format(java.util.Locale.US, lat)}, ${"%.4f".format(java.util.Locale.US, lng)})"
            try {
                val fallbackLabel = reverseGeocodeWithFallback(lat, lng)
                if (fallbackLabel.isNotBlank()) {
                    label = fallbackLabel
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            onResult(label)
        }
    }

    fun abbreviateState(state: String?): String {
        if (state.isNullOrBlank()) return ""
        val s = state.trim()
        if (s.length <= 3) return s.uppercase(java.util.Locale.US)
        
        val lower = s.lowercase(java.util.Locale.US)
        val map = mapOf(
            "nordrhein-westfalen" to "NRW",
            "bayern" to "BY",
            "baden-württemberg" to "BW",
            "berlin" to "BE",
            "brandenburg" to "BB",
            "bremen" to "HB",
            "hamburg" to "HH",
            "hessen" to "HE",
            "mecklenburg-vorpommern" to "MV",
            "niedersachsen" to "NI",
            "rheinland-pfalz" to "RP",
            "saarland" to "SL",
            "sachsen" to "SN",
            "sachsen-anhalt" to "ST",
            "schleswig-holstein" to "SH",
            "thüringen" to "TH",
            "alabama" to "AL", "alaska" to "AK", "arizona" to "AZ", "arkansas" to "AR",
            "california" to "CA", "colorado" to "CO", "connecticut" to "CT", "delaware" to "DE",
            "florida" to "FL", "georgia" to "GA", "hawaii" to "HI", "idaho" to "ID",
            "illinois" to "IL", "indiana" to "IN", "iowa" to "IA", "kansas" to "KS",
            "kentucky" to "KY", "louisiana" to "LA", "maine" to "ME", "maryland" to "MD",
            "massachusetts" to "MA", "michigan" to "MI", "minnesota" to "MN", "mississippi" to "MS",
            "missouri" to "MO", "montana" to "MT", "nebraska" to "NE", "nevada" to "NV",
            "new hampshire" to "NH", "new jersey" to "NJ", "new mexico" to "NM", "new york" to "NY",
            "north carolina" to "NC", "north dakota" to "ND", "ohio" to "OH", "oklahoma" to "OK",
            "oregon" to "OR", "pennsylvania" to "PA", "rhode island" to "RI", "south carolina" to "SC",
            "south dakota" to "SD", "tennessee" to "TN", "texas" to "TX", "utah" to "UT",
            "vermont" to "VT", "virginia" to "VA", "washington" to "WA", "west virginia" to "WV",
            "wisconsin" to "WI", "wyoming" to "WY"
        )
        val matched = map[lower]
        if (matched != null) return matched
        
        val words = s.split(Regex("[\\s\\x2d]+"))
        if (words.size > 1) {
            val initials = words.joinToString("") { it.firstOrNull()?.uppercaseChar()?.toString() ?: "" }
            if (initials.length in 2..3) {
                return initials
            }
        }
        
        return s.take(3).uppercase(java.util.Locale.US)
    }

    fun getCityStateCountryFromLocation(boxId: String, lat: Double, lng: Double, onResult: (String) -> Unit) {
        val cached = boxAddressCache[boxId]
        if (cached != null) {
            onResult(cached)
            return
        }

        viewModelScope.launch {
            var label = ""
            try {
                label = reverseGeocodeWithFallback(lat, lng)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (label.isBlank()) {
                label = "Lat: ${"%.3f".format(java.util.Locale.US, lat)}, Lon: ${"%.3f".format(java.util.Locale.US, lng)}"
            }
            boxAddressCache[boxId] = label
            onResult(label)
        }
    }

    fun getCityStateCountryFullFromLocation(boxId: String, lat: Double, lng: Double, onResult: (String) -> Unit) {
        val cached = boxFullAddressCache[boxId]
        if (cached != null) {
            onResult(cached)
            return
        }

        viewModelScope.launch {
            var label = ""
            try {
                label = reverseGeocodeWithFallback(lat, lng, fullAddress = true)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (label.isBlank()) {
                label = "Lat: ${"%.3f".format(java.util.Locale.US, lat)}, Lon: ${"%.3f".format(java.util.Locale.US, lng)}"
            }
            boxFullAddressCache[boxId] = label
            onResult(label)
        }
    }

    private fun filterBoxes(
        rawList: List<SenseBox>,
        exposure: ExposureFilter,
        lastMinutes: Int,
        radiusKm: Int
    ): List<SenseBox> {
        val now = java.util.Date()
        val refCoords = lastSearchedCoords.value

        return rawList.filter { box ->
            // 1. Exposure filter
            val matchesExposure = when (exposure) {
                ExposureFilter.ALL -> true
                ExposureFilter.OUTDOOR -> box.exposure?.equals("outdoor", ignoreCase = true) == true || box.exposure.isNullOrEmpty()
                ExposureFilter.INDOOR -> box.exposure?.equals("indoor", ignoreCase = true) == true
            }
            if (!matchesExposure) return@filter false

            // 2. Local distance filter
            if (refCoords != null) {
                val boxCoords = box.currentLocation?.coordinates
                if (boxCoords != null && boxCoords.size >= 2) {
                    val boxLon = boxCoords[0]
                    val boxLat = boxCoords[1]
                    val distance = calculateDistanceKm(refCoords.second, refCoords.first, boxLat, boxLon)
                    if (distance > radiusKm) {
                        return@filter false
                    }
                }
            }

            // 3. Last updated filter (999_999 is "All Time")
            if (lastMinutes < 999_999) {
                val maxAgeMs = lastMinutes * 60 * 1000L
                val lastDate = getBoxLastUpdatedDate(box)
                if (lastDate == null) {
                    false
                } else {
                    val ageMs = now.time - lastDate.time
                    ageMs <= maxAgeMs
                }
            } else {
                true
            }
        }
    }

    fun resolveLocationsFor(boxes: List<SenseBox>) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val currentMap = _boxLocations.value.toMutableMap()
            var mapChanged = false
            for (box in boxes) {
                if (currentMap.containsKey(box.id)) continue
                val coords = box.currentLocation?.coordinates
                if (coords != null && coords.size >= 2) {
                    val lng = coords[0]
                    val lat = coords[1]
                    try {
                        val label = reverseGeocodeWithFallback(lat, lng)
                        if (label.isNotBlank()) {
                            currentMap[box.id] = label
                            mapChanged = true
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            if (mapChanged) {
                _boxLocations.value = currentMap.toMap()
            }
        }
    }

    fun selectBox(boxId: String, force: Boolean = false) {
        viewModelScope.launch {
            _errorMessage.value = null

            // If we are selecting a new/different box, reset selectedBox and clear detail-screen cache
            if (_selectedBox.value?.id != boxId) {
                _selectedBox.value = null
                _cachedSensors.value = emptyList()
                clearDetailScreenCache()
            }

            // 1. Try to instantly set _selectedBox from raw discovered list if it exists
            val alreadyDiscovered = _rawDiscoveredBoxes.value.find { it.id == boxId }
            if (alreadyDiscovered != null) {
                _selectedBox.value = alreadyDiscovered
                _cachedSensors.value = repository.getCachedSensors(boxId)
            } else {
                // 2. Otherwise try to check if it's saved in DB and load cache immediately
                val saved = repository.getSavedBox(boxId)
                if (saved != null) {
                    val sensors = repository.getCachedSensors(boxId)
                    _cachedSensors.value = sensors
                    _selectedBox.value = SenseBox(
                        id = saved.boxId,
                        name = saved.name,
                        description = saved.description,
                        exposure = saved.exposure,
                        model = "Offline Cache",
                        grouptagRaw = null,
                        currentLocation = de.nichu42.boxviewer.data.api.CurrentLocation(
                            type = "Point",
                            coordinates = listOf(saved.longitude, saved.latitude)
                        ),
                        sensors = sensors.map {
                            de.nichu42.boxviewer.data.api.Sensor(
                                id = it.sensorId,
                                title = it.sensorTitle,
                                unit = it.sensorUnit,
                                sensorType = it.sensorType,
                                lastMeasurement = de.nichu42.boxviewer.data.api.Measurement(
                                    value = it.value,
                                    createdAt = it.updatedAt
                                )
                            )
                        }
                    )
                }
            }

            val hasInitialValue = _selectedBox.value?.id == boxId
            _isLoading.value = true

            try {
                val box = repository.fetchAndSyncBox(boxId, force)
                _selectedBox.value = box
                _cachedSensors.value = repository.getCachedSensors(boxId)
                SenseBoxWidgetProvider.updateAllWidgetsFromCache(getApplication())
            } catch (e: Exception) {
                e.printStackTrace()
                if (!hasInitialValue) {
                    _errorMessage.value = "Failed to load box details: ${e.message}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleFavorite(box: SenseBox) {
        viewModelScope.launch {
            try {
                val isSaved = repository.getSavedBox(box.id) != null
                if (isSaved) {
                    repository.unfavoriteBox(box.id)
                    SenseBoxWidgetProvider.updateAllWidgetsFromCache(getApplication())
                } else {
                    repository.favoriteBox(box)
                    try {
                        repository.fetchAndSyncBox(box.id, true)
                        SenseBoxWidgetProvider.updateAllWidgetsFromCache(getApplication())
                    } catch (fetchEx: Exception) {
                        fetchEx.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Failed to update favorite: ${e.message}"
            }
        }
    }

    fun unfavoriteBox(boxId: String) {
        viewModelScope.launch {
            try {
                repository.unfavoriteBox(boxId)
                SenseBoxWidgetProvider.updateAllWidgetsFromCache(getApplication())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun searchBoxes(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        lastSearchedCoords.value = null
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _searchRadiusUsed.value = null
            _hasSearchBeenDone.value = true
            try {
                // Check if the query is a 24-character hexadecimal string representing a senseBox ID
                val isId = trimmed.length == 24 && trimmed.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
                if (isId) {
                    try {
                        val directBox = repository.fetchAndSyncBox(trimmed, true)
                        _rawDiscoveredBoxes.value = listOf(directBox)
                        resolveLocationsFor(listOf(directBox))
                        return@launch
                    } catch (idEx: Exception) {
                        idEx.printStackTrace()
                        // Fallback to searching by name if ID lookup failed
                    }
                }

                val results = repository.searchBoxes(trimmed)
                _rawDiscoveredBoxes.value = results
                resolveLocationsFor(results)
                if (results.isEmpty()) {
                    _errorMessage.value = "No senseBoxes found matching \"$query\""
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Failed to search boxes: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun findBoxesNear(longitude: Double, latitude: Double, maxDistanceMeters: Int = 25000) {
        lastSearchedCoords.value = Pair(longitude, latitude)
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _searchRadiusUsed.value = null
            _hasSearchBeenDone.value = true
            try {
                var found = false
                val steps = listOf(25000, 50000, 100000, 250000, 500000)
                val activeSteps = steps.filter { it >= maxDistanceMeters }
                
                for (radius in activeSteps) {
                    val results = repository.findNearBoxes(longitude, latitude, radius)
                    if (results.isNotEmpty()) {
                        _rawDiscoveredBoxes.value = results
                        resolveLocationsFor(results)
                        _searchRadiusUsed.value = radius
                        _searchRadiusKm.value = radius / 1000
                        found = true
                        break
                    }
                }
                
                if (!found) {
                    _rawDiscoveredBoxes.value = emptyList()
                    _errorMessage.value = "No nearby senseBoxes found within 500 km."
                    _searchRadiusUsed.value = 500000
                    _searchRadiusKm.value = 500
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Failed to find nearby boxes: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    @Suppress("DEPRECATION")
    fun searchByLocation(locationName: String) {
        if (locationName.trim().isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _searchRadiusUsed.value = null
            _hasSearchBeenDone.value = true
            try {
                val geocoder = android.location.Geocoder(getApplication(), java.util.Locale.getDefault())
                val addresses = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        geocoder.getFromLocationName(locationName, 1)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }

                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val lat = address.latitude
                    val lng = address.longitude
                    lastSearchedCoords.value = Pair(lng, lat)
                    
                    var found = false
                    val steps = listOf(25000, 50000, 100000, 250000, 500000)
                    for (radius in steps) {
                        val results = repository.findNearBoxes(lng, lat, radius)
                        if (results.isNotEmpty()) {
                            _rawDiscoveredBoxes.value = results
                            resolveLocationsFor(results)
                            _searchRadiusUsed.value = radius
                            _searchRadiusKm.value = radius / 1000
                            found = true
                            break
                        }
                    }
                    if (!found) {
                        _rawDiscoveredBoxes.value = emptyList()
                        _errorMessage.value = "No senseBoxes found within 500 km of \"$locationName\"."
                        _searchRadiusUsed.value = 500000
                        _searchRadiusKm.value = 500
                    }
                } else {
                    _rawDiscoveredBoxes.value = emptyList()
                    _errorMessage.value = "Could not find location \"$locationName\" on the map. Please try a different city name."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Failed to search location: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateRadiusAndQuery(radiusKm: Int, isLocationSearchMode: Boolean) {
        _searchRadiusKm.value = radiusKm
        if (!isLocationSearchMode) return
        val coords = lastSearchedCoords.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _searchRadiusUsed.value = radiusKm * 1000
            _searchRadiusKm.value = radiusKm
            _hasSearchBeenDone.value = true
            try {
                val results = repository.findNearBoxes(coords.first, coords.second, radiusKm * 1000)
                _rawDiscoveredBoxes.value = results
                resolveLocationsFor(results)
                if (results.isEmpty()) {
                    _errorMessage.value = "No senseBoxes found within $radiusKm km of this location."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Failed to fetch boxes for radius $radiusKm km: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshAll(force: Boolean = false) {
        if (savedBoxes.value.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.refreshAllSavedBoxes(force)
                SenseBoxWidgetProvider.updateAllWidgetsFromCache(getApplication())
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Failed to refresh: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getCachedSensorsFlow(boxId: String): kotlinx.coroutines.flow.Flow<List<SensorCacheEntity>> {
        return repository.getCachedSensorsFlow(boxId).combine(aqiSystem) { list: List<SensorCacheEntity>, system: AqiSystem ->
            AqiCalculator.synthesizeVirtualSensors(list, system, boxId)
                .sortedWith(compareBy({ SensorSortKey.of(it.sensorTitle) }, { it.sensorTitle }))
        }
    }

    fun updateDashboardSensors(boxId: String, sensorIds: List<String>) {
        viewModelScope.launch {
            try {
                repository.updateDashboardSensors(boxId, sensorIds)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateSavedBoxesOrder(boxes: List<de.nichu42.boxviewer.data.db.SavedBoxEntity>) {
        viewModelScope.launch {
            try {
                repository.updateSavedBoxesOrder(boxes)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getSensorData(boxId: String, sensorId: String, limit: Int = 20): List<de.nichu42.boxviewer.data.api.Measurement> {
        if (sensorId == "virtual_aqi") {
            val sensors = repository.getCachedSensors(boxId)
            val pm25 = sensors.firstOrNull { 
                val title = it.sensorTitle.lowercase()
                title.contains("pm2.5") || title.contains("pm25") || (it.sensorUnit?.contains("g/m") == true && !title.contains("pm10"))
            }
            val pm10 = sensors.firstOrNull { 
                val title = it.sensorTitle.lowercase()
                title.contains("pm10") || (it.sensorUnit?.contains("g/m") == true && title.contains("pm10"))
            }
            
            val targetSensor = pm25 ?: pm10 ?: return emptyList()
            return repository.getSensorData(boxId, targetSensor.sensorId, limit)
        }
        return repository.getSensorData(boxId, sensorId, limit)
    }

    fun calculateInstantCastForBox(value: Double?): AqiResult {
        // Access the already-loaded in-memory sensor list synchronously — no blocking DB call needed.
        val hasPm25 = _cachedSensors.value.any {
            val title = it.sensorTitle.lowercase()
            title.contains("pm2.5") || title.contains("pm25") || (it.sensorUnit?.contains("g/m") == true && !title.contains("pm10"))
        }
        val pmType = if (hasPm25) "pm2.5" else "pm10"
        return AqiCalculator.calculateInstantCast(value, pmType, aqiSystem.value)
    }

    fun calculateNowCastForBox(values: List<Double>): AqiResult {
        val hasPm25 = _cachedSensors.value.any {
            val title = it.sensorTitle.lowercase()
            title.contains("pm2.5") || title.contains("pm25") || (it.sensorUnit?.contains("g/m") == true && !title.contains("pm10"))
        }
        val pmType = if (hasPm25) "pm2.5" else "pm10"
        return AqiCalculator.calculateNowCast(values, pmType, aqiSystem.value)
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SenseBoxViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SenseBoxViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
