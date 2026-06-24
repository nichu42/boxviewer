package de.nichu42.boxviewer.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenSenseMapApi {
    @GET("boxes/{boxId}")
    suspend fun getBox(
        @Path("boxId") boxId: String
    ): SenseBox

    @GET("boxes")
    suspend fun getBoxesNear(
        @Query("near") near: String, // "longitude,latitude"
        @Query("maxDistance") maxDistance: Int // in meters
    ): List<SenseBox>

    @GET("boxes")
    suspend fun searchBoxes(
        @Query("name") name: String
    ): List<SenseBox>

    @GET("boxes/{boxId}/data/{sensorId}")
    suspend fun getSensorData(
        @Path("boxId") boxId: String,
        @Path("sensorId") sensorId: String,
        @Query("limit") limit: Int = 20,
        @Query("format") format: String = "json"
    ): List<Measurement>
}
