package de.nichu42.boxviewer.data.db

import android.content.Context
import androidx.core.content.edit
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

const val DB_VERSION = 8

@Database(
    entities = [
        SavedBoxEntity::class,
        WidgetConfigEntity::class,
        SensorCacheEntity::class
    ],
    version = DB_VERSION,
    exportSchema = false
)
abstract class SenseBoxDatabase : RoomDatabase() {
    abstract fun savedBoxDao(): SavedBoxDao
    abstract fun widgetConfigDao(): WidgetConfigDao
    abstract fun sensorCacheDao(): SensorCacheDao

    companion object {

        @Volatile
        private var INSTANCE: SenseBoxDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // In version 4, localFetchedAt column was added to sensor_caches table
                db.execSQL("ALTER TABLE sensor_caches ADD COLUMN localFetchedAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // In version 5, metricDisplayMode, showRefreshButton, and showConfigButton columns are added to widget_configs table
                db.execSQL("ALTER TABLE widget_configs ADD COLUMN metricDisplayMode TEXT NOT NULL DEFAULT 'LABEL_VALUE_UNIT'")
                db.execSQL("ALTER TABLE widget_configs ADD COLUMN showRefreshButton INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE widget_configs ADD COLUMN showConfigButton INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // In version 6, showBoxName and showUpdateTime columns are added to widget_configs table
                db.execSQL("ALTER TABLE widget_configs ADD COLUMN showBoxName INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE widget_configs ADD COLUMN showUpdateTime INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // In version 7, useConditionalFormatting column is added to widget_configs table
                db.execSQL("ALTER TABLE widget_configs ADD COLUMN useConditionalFormatting INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // In version 8, aqiDisplayMode column is added to widget_configs table
                db.execSQL("ALTER TABLE widget_configs ADD COLUMN aqiDisplayMode TEXT NOT NULL DEFAULT 'NUMBER_AND_LABEL'")
            }
        }

        fun getDatabase(context: Context): SenseBoxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SenseBoxDatabase::class.java,
                    "sensebox_database"
                )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .addCallback(object : Callback() {
                    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                        super.onDestructiveMigration(db)
                        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                            .edit { putBoolean("db_reset_occurred", true) }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
