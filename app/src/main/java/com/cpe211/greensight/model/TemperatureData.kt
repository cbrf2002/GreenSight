package com.cpe211.greensight.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "temperature_data")
data class TemperatureData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val temperatureValue: Float,
    val timestamp: Long = System.currentTimeMillis() // Store timestamp as Long
)