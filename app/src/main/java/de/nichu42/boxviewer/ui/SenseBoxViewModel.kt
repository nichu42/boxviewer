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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class SenseBoxViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SenseBoxDatabase.getDatabase(application)
    private val repository = SenseBoxRepository(db)

    val savedBoxes: StateFlow<List<de.nichu42.boxviewer.data.db.SavedBoxEntity>> = repository.savedBoxes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    enum class ExposureFilter(val label: String) {
        OUTDOOR("Outdoor"),
        INDOOR("Indoor"),
        ALL("All")
    }

    private val _rawDiscoveredBoxes = MutableStateFlow<List<SenseBox>>(emptyList())
    val rawDiscoveredBoxes: StateFlow<List<SenseBox>> = _rawDiscoveredBoxes.asStateFlow()
    
    val selectedExposure = MutableStateFlow(ExposureFilter.OUTDOOR)
    
    private val _lastUpdatedHours = MutableStateFlow(1) // Default 1 hour
    val lastUpdatedHours: StateFlow<Int> = _lastUpdatedHours.asStateFlow()

    fun setLastUpdatedHours(hours: Int) {
        _lastUpdatedHours.value = hours
    }

    // Reverse-geocoded locations cache mapping boxId -> "City, State, Country"
    private val _boxLocations = MutableStateFlow<Map<String, String>>(emptyMap())
    val boxLocations: StateFlow<Map<String, String>> = _boxLocations.asStateFlow()

    private val _searchRadiusKm = MutableStateFlow(25)
    val searchRadiusKm: StateFlow<Int> = _searchRadiusKm.asStateFlow()

    fun setSearchRadiusKm(km: Int) {
        _searchRadiusKm.value = km
    }

    // Cache for box last updated timestamps and text to avoid repeating expensive formatting/parsing
    private val boxLastUpdatedCache = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val boxLastUpdatedTextCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val boxAddressCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val boxFullAddressCache = java.util.concurrent.ConcurrentHashMap<String, String>()

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

    val discoveredBoxes: StateFlow<List<SenseBox>> = kotlinx.coroutines.flow.combine(
        _rawDiscoveredBoxes,
        selectedExposure,
        _lastUpdatedHours,
        _searchRadiusKm,
        _isLocationSearch
    ) { rawList, exposure, lastHours, radiusKm, isLocSearch ->
        val filtered = if (!isLocSearch) {
            rawList
        } else {
            filterBoxes(rawList, exposure, lastHours, radiusKm)
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
                val nativeAddresses = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocationName(query, 5)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }

                // 2. Supplementary Photon Komoot API query (highly reliable on cloud clusters, no 403 blocks)
                val photonAddresses = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val list = mutableListOf<android.location.Address>()
                    try {
                        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                        val url = "https://photon.komoot.io/api/?q=$encodedQuery&limit=5"
                        val client = okhttp3.OkHttpClient()
                        val request = okhttp3.Request.Builder()
                            .url(url)
                            .header("User-Agent", "SenseBoxFinderApp/1.0")
                            .build()
                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val body = response.body.string()
                                if (body.isNotBlank()) {
                                    val jsonObject = org.json.JSONObject(body)
                                    val features = jsonObject.optJSONArray("features")
                                    if (features != null) {
                                        for (i in 0 until features.length()) {
                                            val feature = features.getJSONObject(i)
                                            val geometry = feature.optJSONObject("geometry")
                                            val properties = feature.optJSONObject("properties")
                                            if (geometry != null && properties != null) {
                                                val coords = geometry.optJSONArray("coordinates")
                                                if (coords != null && coords.length() >= 2) {
                                                    val lon = coords.optDouble(0, 0.0)
                                                    val lat = coords.optDouble(1, 0.0)
                                                    
                                                    val name = properties.optString("name", "")
                                                    val city = properties.optString("city", "")
                                                    val state = properties.optString("state", "")
                                                    val country = properties.optString("country", "")
                                                    
                                                    val labelParts = listOfNotNull(
                                                        name.ifBlank { null },
                                                        city.ifBlank { null },
                                                        state.ifBlank { null },
                                                        country.ifBlank { null }
                                                    ).distinct()
                                                    val displayName = labelParts.joinToString(", ")
                                                    
                                                    if (displayName.isNotBlank()) {
                                                        val address = android.location.Address(java.util.Locale.US).apply {
                                                            latitude = lat
                                                            longitude = lon
                                                            setAddressLine(0, displayName)
                                                            locality = city.ifBlank { name }
                                                            adminArea = state
                                                            countryName = country
                                                        }
                                                        list.add(address)
                                                    }
                                                }
                                            }
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

                // 3. Supplementary Nominatim OSM API query for multiple rich results
                val nominatimAddresses = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val list = mutableListOf<android.location.Address>()
                    try {
                        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                        val url = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=5&accept-language=en"
                        val client = okhttp3.OkHttpClient()
                        val request = okhttp3.Request.Builder()
                            .url(url)
                            .header("User-Agent", "SenseBoxFinderApp/1.0 (contact: nicolai.roediger@gmail.com)")
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
                                            val address = android.location.Address(java.util.Locale.US).apply {
                                                latitude = lat
                                                longitude = lon
                                                setAddressLine(0, displayName)
                                                
                                                val name = obj.optString("name", "")
                                                if (name.isNotBlank()) {
                                                    locality = name
                                                }
                                            }
                                            list.add(address)
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

                // 4. Combine all lists, filter duplicates by label, and take up to 3-5 results
                val combined = (nativeAddresses ?: emptyList()) + photonAddresses + nominatimAddresses
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

    init {
        // No automatic nearby search on startup, but sync saved boxes to make sure their values are up to date!
        viewModelScope.launch {
            try {
                repository.refreshAllSavedBoxes()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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

    fun getBoxLastUpdatedDate(box: SenseBox): java.util.Date? {
        val cached = boxLastUpdatedCache[box.id]
        if (cached != null) {
            return if (cached == -1L) null else java.util.Date(cached)
        }

        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        val sensorsList = box.sensors
        if (sensorsList == null) {
            boxLastUpdatedCache[box.id] = -1L
            return null
        }
        val dates = sensorsList.mapNotNull { sensor ->
            val m = sensor.lastMeasurement ?: return@mapNotNull null
            
            // 1. Try to parse createdAt if available standard string format
            m.createdAt?.let { ts ->
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
                            // try next format
                        }
                    }
                }
            }
            
            // 2. If createdAt is null, try to parse from the value (since list endpoint passes Object ID as value)
            m.value?.let { v ->
                parseObjectIdTimestamp(v)
            }
        }
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

    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
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

    fun getAddressFromLocation(lat: Double, lng: Double, onResult: (String) -> Unit) {
        viewModelScope.launch {
            var label = "Location (${"%.4f".format(java.util.Locale.US, lat)}, ${"%.4f".format(java.util.Locale.US, lng)})"
            try {
                // 1. Try local Android Geocoder reverse lookup
                val nativeAddress = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
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
                    val city = nativeAddress.locality ?: nativeAddress.subAdminArea ?: nativeAddress.subLocality
                    val state = nativeAddress.adminArea
                    val country = nativeAddress.countryName
                    val parts = listOfNotNull(city, state, country).filter { it.isNotBlank() }
                    if (parts.isNotEmpty()) {
                        label = parts.joinToString(", ")
                    } else {
                        val firstLine = nativeAddress.getAddressLine(0)
                        if (!firstLine.isNullOrBlank()) {
                            label = firstLine
                        }
                    }
                } else {
                    // 2. Fallback to Nominatim OSM Reverse API
                    val nominatimLabel = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val url = "https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lng&format=json&accept-language=en"
                            val client = okhttp3.OkHttpClient()
                            val request = okhttp3.Request.Builder()
                                .url(url)
                                .header("User-Agent", "SenseBoxFinderApp/1.0 (contact: nicolai.roediger@gmail.com)")
                                .build()
                            client.newCall(request).execute().use { response ->
                                if (response.isSuccessful) {
                                    val body = response.body.string()
                                    if (body.isNotBlank()) {
                                        val json = org.json.JSONObject(body)
                                        json.optString("display_name", "")
                                    } else ""
                                } else ""
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            ""
                        }
                    }
                    if (nominatimLabel.isNotBlank()) {
                        label = nominatimLabel
                    }
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
                // 1. Try native Geocoder
                val nativeAddress = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
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
                    val countryCode = (nativeAddress.countryCode ?: "").trim().uppercase(java.util.Locale.US)
                    
                    val abbrevState = abbreviateState(state)
                    val parts = listOf(city, abbrevState, countryCode).filter { it.isNotBlank() }
                    if (parts.isNotEmpty()) {
                        label = parts.joinToString(", ")
                    }
                }
                
                // 2. Try Nominatim fallback if native geocoder failed or returned empty
                if (label.isBlank()) {
                    val nominatimLabel = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val url = "https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lng&format=json&accept-language=en"
                            val client = okhttp3.OkHttpClient()
                            val request = okhttp3.Request.Builder()
                                .url(url)
                                .header("User-Agent", "SenseBoxFinderApp/1.0 (contact: nicolai.roediger@gmail.com)")
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
                                            val countryCode = addressObj.optString("country_code", "").trim().uppercase(java.util.Locale.US)
                                            val abbrevState = abbreviateState(state)
                                            val parts = listOf(city, abbrevState, countryCode).filter { it.isNotBlank() }
                                            if (parts.isNotEmpty()) {
                                                parts.joinToString(", ")
                                            } else ""
                                        } else ""
                                    } else ""
                                } else ""
                             }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            ""
                        }
                    }
                    if (nominatimLabel.isNotBlank()) {
                        label = nominatimLabel
                    }
                }
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
                // 1. Try native Geocoder
                val nativeAddress = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
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
                    val countryName = (nativeAddress.countryName ?: "").trim()
                    
                    val parts = listOf(city, state, countryName).filter { it.isNotBlank() }
                    if (parts.isNotEmpty()) {
                        label = parts.joinToString(", ")
                    }
                }
                
                // 2. Try Nominatim fallback if native geocoder failed or returned empty
                if (label.isBlank()) {
                    val nominatimLabel = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val url = "https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lng&format=json&accept-language=en"
                            val client = okhttp3.OkHttpClient()
                            val request = okhttp3.Request.Builder()
                                .url(url)
                                .header("User-Agent", "SenseBoxFinderApp/1.0 (contact: nicolai.roediger@gmail.com)")
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
                                            val country = addressObj.optString("country", "").trim()
                                            val parts = listOf(city, state, country).filter { it.isNotBlank() }
                                            if (parts.isNotEmpty()) {
                                                parts.joinToString(", ")
                                            } else ""
                                        } else ""
                                    } else ""
                                } else ""
                             }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            ""
                        }
                    }
                    if (nominatimLabel.isNotBlank()) {
                        label = nominatimLabel
                    }
                }
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
        lastHours: Int,
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

            // 3. Last updated filter (slider: 1..168, 169 is All Time)
            if (lastHours < 169) {
                val maxAgeMs = lastHours * 60 * 60 * 1000L
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
                        val geocoder = android.location.Geocoder(getApplication(), java.util.Locale.getDefault())
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(lat, lng, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            val city = addr.locality ?: addr.subAdminArea ?: addr.subLocality
                            val state = addr.adminArea
                            val country = addr.countryName
                            val parts = listOfNotNull(city, state, country).filter { it.isNotBlank() }
                            if (parts.isNotEmpty()) {
                                currentMap[box.id] = parts.joinToString(", ")
                                mapChanged = true
                            }
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

            // If we are selecting a new/different box, reset selectedBox if not matches
            if (_selectedBox.value?.id != boxId) {
                _selectedBox.value = null
                _cachedSensors.value = emptyList()
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
                } else {
                    repository.favoriteBox(box)
                    try {
                        repository.fetchAndSyncBox(box.id, true)
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
                val addresses = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
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
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Failed to refresh: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getCachedSensorsFlow(boxId: String): kotlinx.coroutines.flow.Flow<List<SensorCacheEntity>> {
        return repository.getCachedSensorsFlow(boxId)
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
        return repository.getSensorData(boxId, sensorId, limit)
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
