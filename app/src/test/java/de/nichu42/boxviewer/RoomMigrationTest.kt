package de.nichu42.boxviewer

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import de.nichu42.boxviewer.data.db.SavedBoxEntity
import de.nichu42.boxviewer.data.db.SenseBoxDatabase
import de.nichu42.boxviewer.data.db.SensorCacheEntity
import de.nichu42.boxviewer.data.db.WidgetConfigEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Guards the Room migration chain (MIGRATION_3_4 .. MIGRATION_7_8).
 *
 * Per project rules, database upgrades must never destroy user data, so this test
 * builds a raw SQLite database that mirrors the v3 schema as shipped before the
 * migration chain existed (reconstructed from the ALTER TABLE statements each
 * Migration executes), seeds user data into it, then opens it through Room and
 * verifies that Room's schema validation passes AND every seed row survived.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migration-test.db"

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    /** Schema of widget_configs at version 3: none of the later ADD COLUMN columns exist yet. */
    private fun createLegacyV3Database() {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `saved_boxes` (" +
                            "`boxId` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, " +
                            "`exposure` TEXT, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, " +
                            "`savedAt` INTEGER NOT NULL, `dashboardSensorIds` TEXT, PRIMARY KEY(`boxId`))"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `widget_configs` (" +
                            "`widgetId` INTEGER NOT NULL, `boxId` TEXT NOT NULL, `boxName` TEXT NOT NULL, " +
                            "`sensorIdsString` TEXT NOT NULL, `refreshIntervalMinutes` INTEGER NOT NULL, " +
                            "`visualizationType` TEXT NOT NULL, `themeColorIndex` INTEGER NOT NULL, " +
                            "`lastFetchedTime` INTEGER NOT NULL, `textScale` REAL NOT NULL, PRIMARY KEY(`widgetId`))"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `sensor_caches` (" +
                            "`sensorId` TEXT NOT NULL, `boxId` TEXT NOT NULL, `sensorTitle` TEXT NOT NULL, " +
                            "`sensorUnit` TEXT, `sensorType` TEXT, `value` TEXT, `updatedAt` TEXT, " +
                            "PRIMARY KEY(`sensorId`))"
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        FrameworkSQLiteOpenHelperFactory().create(configuration).use { helper ->
            val db = helper.writableDatabase
            db.execSQL(
                "INSERT INTO saved_boxes " +
                    "(boxId, name, description, exposure, latitude, longitude, savedAt, dashboardSensorIds) VALUES " +
                    "('box_legacy', 'Legacy Box', 'Pre-migration box', 'outdoor', 52.51, 13.37, 1700000000000, 'temp,hum')"
            )
            db.execSQL(
                "INSERT INTO widget_configs " +
                    "(widgetId, boxId, boxName, sensorIdsString, refreshIntervalMinutes, visualizationType, themeColorIndex, lastFetchedTime, textScale) VALUES " +
                    "(42, 'box_legacy', 'Legacy Box', 'temp', 30, 'LIST', 0, 0, 1.0)"
            )
            db.execSQL(
                "INSERT INTO sensor_caches " +
                    "(sensorId, boxId, sensorTitle, sensorUnit, sensorType, value, updatedAt) VALUES " +
                    "('temp', 'box_legacy', 'Temperature', '°C', 'temperature', '21.5', '2026-06-10T12:00:00Z')"
            )
        }
    }

    private fun openWithMigrations(): SenseBoxDatabase =
        Room.databaseBuilder(context, SenseBoxDatabase::class.java, dbName)
            .addMigrations(
                SenseBoxDatabase.MIGRATION_3_4,
                SenseBoxDatabase.MIGRATION_4_5,
                SenseBoxDatabase.MIGRATION_5_6,
                SenseBoxDatabase.MIGRATION_6_7,
                SenseBoxDatabase.MIGRATION_7_8
            )
            .allowMainThreadQueries()
            .build()

    @Test
    fun migrationChain3To8_passesRoomSchemaValidation_andPreservesUserData() = runBlocking {
        createLegacyV3Database()
        val db = openWithMigrations()

        // Saved favorite must survive untouched.
        // NOTE: if any migration output mismatches the entities, Room throws
        // IllegalStateException("Migration didn't properly handle ...") on this open/query.
        val box = db.savedBoxDao().getSavedBox("box_legacy")
        assertNotNull("Favorite saved box lost during migration", box)
        assertEquals("Legacy Box", box!!.name)
        assertEquals("temp,hum", box.dashboardSensorIds)

        // Widget config must survive; columns added by migrations carry their defaults.
        val config = db.widgetConfigDao().getWidgetConfig(42)
        assertNotNull("Widget config lost during migration", config)
        assertEquals("box_legacy", config!!.boxId)
        assertEquals(30, config.refreshIntervalMinutes)
        assertEquals("LABEL_VALUE_UNIT", config.metricDisplayMode)
        assertTrue(config.showRefreshButton)
        assertTrue(config.showConfigButton)
        assertTrue(config.showBoxName)
        assertTrue(config.showUpdateTime)
        assertTrue(config.useConditionalFormatting)
        assertEquals("NUMBER_AND_LABEL", config.aqiDisplayMode)

        // Sensor cache must survive; localFetchedAt was added by 3→4 with DEFAULT 0.
        val sensors = db.sensorCacheDao().getCachedSensors("box_legacy")
        assertEquals(1, sensors.size)
        assertEquals("Temperature", sensors[0].sensorTitle)
        assertEquals("21.5", sensors[0].value)
        assertEquals(0L, sensors[0].localFetchedAt)

        db.close()
    }

    @Test
    fun freshInstall_currentEntitiesRoundTripThroughDaos() = runBlocking {
        val db = openWithMigrations()

        val now = System.currentTimeMillis()
        db.savedBoxDao().insertSavedBox(
            SavedBoxEntity(
                boxId = "box_fresh",
                name = "Fresh Box",
                description = null,
                exposure = "indoor",
                latitude = 48.13,
                longitude = 11.58,
                savedAt = now,
                dashboardSensorIds = "temp"
            )
        )
        db.widgetConfigDao().insertWidgetConfig(
            WidgetConfigEntity(widgetId = 7, boxId = "box_fresh", boxName = "Fresh Box", sensorIdsString = "temp")
        )
        db.sensorCacheDao().insertSensors(
            listOf(
                SensorCacheEntity(
                    sensorId = "temp",
                    boxId = "box_fresh",
                    sensorTitle = "Temperature",
                    sensorUnit = "°C",
                    sensorType = "temperature",
                    value = "19.8",
                    updatedAt = "2026-06-10T12:00:00Z",
                    localFetchedAt = now
                )
            )
        )

        assertEquals("Fresh Box", db.savedBoxDao().getSavedBox("box_fresh")?.name)
        assertTrue(db.widgetConfigDao().getWidgetConfig(7)?.showBoxName == true)
        assertEquals(now, db.sensorCacheDao().getCachedSensors("box_fresh")[0].localFetchedAt)

        db.close()
    }
}
