package com.cpe211.greensight.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cpe211.greensight.db.dao.HumidityDataDao
import com.cpe211.greensight.db.dao.TemperatureDataDao
import com.cpe211.greensight.model.TemperatureData
import com.cpe211.greensight.model.HumidityData

@Database(entities = [TemperatureData::class, HumidityData::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun temperatureDataDao(): TemperatureDataDao
    abstract fun humidityDataDao(): HumidityDataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}