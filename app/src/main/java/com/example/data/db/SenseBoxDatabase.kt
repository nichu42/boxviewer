package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SavedBoxEntity::class,
        WidgetConfigEntity::class,
        SensorCacheEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class SenseBoxDatabase : RoomDatabase() {
    abstract fun savedBoxDao(): SavedBoxDao
    abstract fun widgetConfigDao(): WidgetConfigDao
    abstract fun sensorCacheDao(): SensorCacheDao

    companion object {
        @Volatile
        private var INSTANCE: SenseBoxDatabase? = null

        fun getDatabase(context: Context): SenseBoxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SenseBoxDatabase::class.java,
                    "sensebox_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
