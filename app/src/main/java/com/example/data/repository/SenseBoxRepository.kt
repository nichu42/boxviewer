package com.example.data.repository

import com.example.data.api.OpenSenseMapApi
import com.example.data.api.RetrofitClient
import com.example.data.api.SenseBox
import com.example.data.db.*
import com.example.util.ApiLogger
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SenseBoxRepository(private val db: SenseBoxDatabase) {
    private val api = RetrofitClient.api
    private val savedBoxDao = db.savedBoxDao()
    private val widgetConfigDao = db.widgetConfigDao()
    private val sensorCacheDao = db.sensorCacheDao()

    val savedBoxes: Flow<List<SavedBoxEntity>> = savedBoxDao.getAllSavedBoxesFlow()

    fun getCachedSensorsFlow(boxId: String): Flow<List<SensorCacheEntity>> =
        sensorCacheDao.getCachedSensorsFlow(boxId)

    suspend fun getSavedBoxesList(): List<SavedBoxEntity> = withContext(Dispatchers.IO) {
        savedBoxDao.getAllSavedBoxes()
    }

    suspend fun getSavedBox(boxId: String): SavedBoxEntity? = withContext(Dispatchers.IO) {
        savedBoxDao.getSavedBox(boxId)
    }

    suspend fun getCachedSensors(boxId: String): List<SensorCacheEntity> = withContext(Dispatchers.IO) {
        sensorCacheDao.getCachedSensors(boxId)
    }

    suspend fun getWidgetConfig(widgetId: Int): WidgetConfigEntity? = withContext(Dispatchers.IO) {
        widgetConfigDao.getWidgetConfig(widgetId)
    }

    fun getWidgetConfigFlow(widgetId: Int): Flow<WidgetConfigEntity?> =
        widgetConfigDao.getWidgetConfigFlow(widgetId)

    suspend fun saveWidgetConfig(config: WidgetConfigEntity) = withContext(Dispatchers.IO) {
        widgetConfigDao.insertWidgetConfig(config)
    }

    suspend fun deleteWidgetConfig(widgetId: Int) = withContext(Dispatchers.IO) {
        widgetConfigDao.deleteWidgetConfig(widgetId)
    }

    suspend fun getAllWidgetConfigs(): List<WidgetConfigEntity> = withContext(Dispatchers.IO) {
        widgetConfigDao.getAllWidgetConfigs()
    }

    /**
     * Fetches a box from openSenseMap. If it is already in SavedBox table,
     * updates the database metadata and sensor caches.
     */
    suspend fun fetchAndSyncBox(boxId: String, force: Boolean = false): SenseBox = withContext(Dispatchers.IO) {
        val cached = sensorCacheDao.getCachedSensors(boxId)
        val now = System.currentTimeMillis()
        val isFresh = cached.isNotEmpty() && cached.any { (now - it.localFetchedAt) < 60000 }

        if (!force && isFresh) {
            val saved = savedBoxDao.getSavedBox(boxId)
            return@withContext SenseBox(
                id = boxId,
                name = saved?.name ?: "senseBox",
                description = saved?.description,
                exposure = saved?.exposure,
                model = "Cached",
                grouptagRaw = null,
                currentLocation = com.example.data.api.CurrentLocation(
                    type = "Point",
                    coordinates = listOf(saved?.longitude ?: 0.0, saved?.latitude ?: 0.0)
                ),
                sensors = cached.map {
                    com.example.data.api.Sensor(
                        id = it.sensorId,
                        title = it.sensorTitle,
                        unit = it.sensorUnit,
                        sensorType = it.sensorType,
                        lastMeasurement = com.example.data.api.Measurement(
                            value = it.value,
                            createdAt = it.updatedAt
                        )
                    )
                }
            )
        }

        val url = "https://api.opensensemap.org/boxes/$boxId"
        val box = logApiCall("GET", url) {
            api.getBox(boxId)
        }
        
        // Cache sensors
        val caches = box.sensors?.map { sensor ->
            SensorCacheEntity(
                sensorId = sensor.id,
                boxId = box.id,
                sensorTitle = sensor.title,
                sensorUnit = sensor.unit,
                sensorType = sensor.sensorType,
                value = sensor.lastMeasurement?.value,
                updatedAt = sensor.lastMeasurement?.createdAt,
                localFetchedAt = System.currentTimeMillis()
            )
        } ?: emptyList()
        
        if (caches.isNotEmpty()) {
            sensorCacheDao.insertSensors(caches)
        }

        // If saved, update SavedBoxEntity details
        val saved = savedBoxDao.getSavedBox(boxId)
        if (saved != null) {
            savedBoxDao.insertSavedBox(
                SavedBoxEntity(
                    boxId = box.id,
                    name = box.name,
                    description = box.description,
                    exposure = box.exposure,
                    latitude = box.currentLocation?.latitude ?: saved.latitude,
                    longitude = box.currentLocation?.longitude ?: saved.longitude,
                    savedAt = saved.savedAt, // keep original order of selection
                    dashboardSensorIds = saved.dashboardSensorIds
                )
            )
        }

        box
    }

    suspend fun favoriteBox(box: SenseBox) = withContext(Dispatchers.IO) {
        val entity = SavedBoxEntity(
            boxId = box.id,
            name = box.name,
            description = box.description,
            exposure = box.exposure,
            latitude = box.currentLocation?.latitude ?: 0.0,
            longitude = box.currentLocation?.longitude ?: 0.0,
            dashboardSensorIds = null
        )
        savedBoxDao.insertSavedBox(entity)

        val caches = box.sensors?.map { sensor ->
            SensorCacheEntity(
                sensorId = sensor.id,
                boxId = box.id,
                sensorTitle = sensor.title,
                sensorUnit = sensor.unit,
                sensorType = sensor.sensorType,
                value = sensor.lastMeasurement?.value,
                updatedAt = sensor.lastMeasurement?.createdAt,
                localFetchedAt = System.currentTimeMillis()
            )
        } ?: emptyList()

        if (caches.isNotEmpty()) {
            sensorCacheDao.insertSensors(caches)
        }
    }

    suspend fun unfavoriteBox(boxId: String) = withContext(Dispatchers.IO) {
        savedBoxDao.deleteSavedBox(boxId)
        sensorCacheDao.deleteCachedSensors(boxId)
    }

    suspend fun searchBoxes(query: String): List<SenseBox> = withContext(Dispatchers.IO) {
        if (query.trim().isEmpty()) return@withContext emptyList()
        val url = "https://api.opensensemap.org/boxes".toHttpUrlOrNull()!!
            .newBuilder()
            .addQueryParameter("name", query)
            .build()
            .toString()
        logApiCall("GET", url) {
            api.searchBoxes(query)
        }
    }

    suspend fun findNearBoxes(longitude: Double, latitude: Double, maxDistanceMeters: Int): List<SenseBox> = withContext(Dispatchers.IO) {
        val url = "https://api.opensensemap.org/boxes".toHttpUrlOrNull()!!
            .newBuilder()
            .addQueryParameter("near", "$longitude,$latitude")
            .addQueryParameter("maxDistance", maxDistanceMeters.toString())
            .build()
            .toString()
        logApiCall("GET", url) {
            api.getBoxesNear("$longitude,$latitude", maxDistanceMeters)
        }
    }

    suspend fun refreshAllSavedBoxes(force: Boolean = false) = withContext(Dispatchers.IO) {
        val saved = savedBoxDao.getAllSavedBoxes()
        for (box in saved) {
            try {
                fetchAndSyncBox(box.boxId, force)
            } catch (e: Exception) {
                // Ignore single box failure during batch sync
                e.printStackTrace()
            }
        }
    }

    suspend fun updateDashboardSensors(boxId: String, sensorIds: List<String>) = withContext(Dispatchers.IO) {
        val saved = savedBoxDao.getSavedBox(boxId)
        if (saved != null) {
            val updated = saved.copy(dashboardSensorIds = sensorIds.joinToString(","))
            savedBoxDao.insertSavedBox(updated)
        }
    }

    suspend fun updateSavedBoxesOrder(boxes: List<SavedBoxEntity>) = withContext(Dispatchers.IO) {
        val baseTime = System.currentTimeMillis()
        val updatedBoxes = boxes.mapIndexed { index, box ->
            box.copy(savedAt = baseTime - index * 1000)
        }
        savedBoxDao.insertSavedBoxes(updatedBoxes)
    }

    suspend fun getSensorData(boxId: String, sensorId: String, limit: Int = 20): List<com.example.data.api.Measurement> = withContext(Dispatchers.IO) {
        val url = ("https://api.opensensemap.org/boxes/$boxId/data/$sensorId").toHttpUrlOrNull()!!
            .newBuilder()
            .addQueryParameter("limit", limit.toString())
            .addQueryParameter("format", "json")
            .build()
            .toString()
        logApiCall("GET", url) {
            api.getSensorData(boxId, sensorId, limit)
        }
    }

    private suspend fun <T> logApiCall(
        method: String,
        url: String,
        call: suspend () -> T
    ): T {
        if (!ApiLogger.isLoggingEnabled()) {
            return call()
        }

        val startTime = System.currentTimeMillis()
        var status = 0
        var parsingResult: String? = null
        var errorMsg: String? = null

        try {
            val result = call()
            status = 200
            
            parsingResult = when (result) {
                is SenseBox -> {
                    "Success: Parsed Box '${result.name}' (id=${result.id}) with ${result.sensors?.size ?: 0} sensors: " +
                            (result.sensors?.joinToString { "${it.title}=${it.lastMeasurement?.value ?: "null"}" } ?: "none")
                }
                is List<*> -> {
                    if (result.isEmpty()) {
                        "Success: Parsed empty list"
                    } else {
                        val first = result.firstOrNull()
                        when (first) {
                            is SenseBox -> {
                                "Success: Parsed List of ${result.size} Boxes. Names: " +
                                        result.joinToString(limit = 5) { (it as SenseBox).name }
                            }
                            is com.example.data.api.Measurement -> {
                                "Success: Parsed List of ${result.size} Measurements. First: value=${first.value}, createdAt=${first.createdAt}"
                            }
                            else -> "Success: Parsed List of ${result.size} items of type ${first?.javaClass?.simpleName}"
                        }
                    }
                }
                else -> "Success: Parsed object of type ${result?.javaClass?.simpleName}"
            }
            return result
        } catch (e: Exception) {
            errorMsg = e.javaClass.name + ": " + e.message
            
            if (e is retrofit2.HttpException) {
                status = e.code()
                parsingResult = "HTTP Error: ${e.message()}"
            } else if (e is com.squareup.moshi.JsonDataException || e is com.squareup.moshi.JsonEncodingException) {
                status = 200
                parsingResult = "Parsing Failed: " + e.javaClass.simpleName + " - " + e.message
            } else {
                status = 0
                parsingResult = "Request Failed"
            }
            throw e
        } finally {
            val duration = System.currentTimeMillis() - startTime
            val responseJson = ApiLogger.responseCache.remove(url)
            
            ApiLogger.logRequest(
                method = method,
                url = url,
                status = status,
                durationMs = duration,
                responseJson = responseJson,
                parsingResult = parsingResult,
                error = errorMsg
            )
        }
    }
}
