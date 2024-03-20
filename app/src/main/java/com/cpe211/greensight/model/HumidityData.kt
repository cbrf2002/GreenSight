package com.cpe211.greensight.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "humidity_data")
data class HumidityData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val humidityValue: Float,
    val timestamp: Long = System.currentTimeMillis() // Store timestamp as Long
)