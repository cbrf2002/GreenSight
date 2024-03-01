package com.cpe211.greensight.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cpe211.greensight.model.HumidityData

@Dao
interface HumidityDataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(humidityData: HumidityData)

    @Query("SELECT * FROM humidity_data ORDER BY timestamp DESC")
    suspend fun getAllHumidityData(): List<HumidityData>

    @Query("SELECT * FROM humidity_data WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getHumidityDataInRange(startTime: Long, endTime: Long): List<HumidityData>
}