package de.nichu42.boxviewer.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedBoxDao {
    @Query("SELECT * FROM saved_boxes ORDER BY savedAt DESC")
    fun getAllSavedBoxesFlow(): Flow<List<SavedBoxEntity>>

    @Query("SELECT * FROM saved_boxes ORDER BY savedAt DESC")
    suspend fun getAllSavedBoxes(): List<SavedBoxEntity>

    @Query("SELECT * FROM saved_boxes WHERE boxId = :boxId LIMIT 1")
    suspend fun getSavedBox(boxId: String): SavedBoxEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedBox(box: SavedBoxEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedBoxes(boxes: List<SavedBoxEntity>)

    @Query("DELETE FROM saved_boxes WHERE boxId = :boxId")
    suspend fun deleteSavedBox(boxId: String)

    @Query("UPDATE saved_boxes SET addressShort = :shortLabel, addressFetchedAt = :fetchedAt WHERE boxId = :boxId")
    suspend fun updateShortAddress(boxId: String, shortLabel: String, fetchedAt: Long)

    @Query("UPDATE saved_boxes SET addressFull = :fullLabel, addressFetchedAt = :fetchedAt WHERE boxId = :boxId")
    suspend fun updateFullAddress(boxId: String, fullLabel: String, fetchedAt: Long)

    @Query("UPDATE saved_boxes SET addressShort = :shortLabel, addressFull = :fullLabel, addressFetchedAt = :fetchedAt WHERE boxId = :boxId")
    suspend fun updateAddresses(boxId: String, shortLabel: String?, fullLabel: String?, fetchedAt: Long)
}

@Dao
interface WidgetConfigDao {
    @Query("SELECT * FROM widget_configs WHERE widgetId = :widgetId LIMIT 1")
    fun getWidgetConfigFlow(widgetId: Int): Flow<WidgetConfigEntity?>

    @Query("SELECT * FROM widget_configs WHERE widgetId = :widgetId LIMIT 1")
    suspend fun getWidgetConfig(widgetId: Int): WidgetConfigEntity?

    @Query("SELECT * FROM widget_configs")
    suspend fun getAllWidgetConfigs(): List<WidgetConfigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWidgetConfig(config: WidgetConfigEntity)

    @Query("DELETE FROM widget_configs WHERE widgetId = :widgetId")
    suspend fun deleteWidgetConfig(widgetId: Int)
}

@Dao
interface SensorCacheDao {
    @Query("SELECT * FROM sensor_caches WHERE boxId = :boxId")
    fun getCachedSensorsFlow(boxId: String): Flow<List<SensorCacheEntity>>

    @Query("SELECT * FROM sensor_caches WHERE boxId = :boxId")
    suspend fun getCachedSensors(boxId: String): List<SensorCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSensors(sensors: List<SensorCacheEntity>)

    @Query("DELETE FROM sensor_caches WHERE boxId = :boxId")
    suspend fun deleteCachedSensors(boxId: String)
}
