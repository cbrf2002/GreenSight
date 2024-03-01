package com.cpe211.greensight.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cpe211.greensight.model.TemperatureData

@Dao
interface TemperatureDataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(temperatureData: TemperatureData)

    @Query("SELECT * FROM temperature_data ORDER BY timestamp DESC")
    suspend fun getAllTemperatureData(): List<TemperatureData>

    @Query("SELECT * FROM temperature_data WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getTemperatureDataInRange(startTime: Long, endTime: Long): List<TemperatureData>
}