package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SavedBoxEntity::class,
        WidgetConfigEntity::class,
        SensorCacheEntity::class
    ],
    version = 5,
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

        fun getDatabase(context: Context): SenseBoxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SenseBoxDatabase::class.java,
                    "sensebox_database"
                )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                        super.onDestructiveMigration(db)
                        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean("db_reset_occurred", true)
                            .apply()
                    }
                })
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
