package com.cpe211.greensight

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.app.Application
import android.content.Context
import android.widget.TextView
import androidx.lifecycle.AndroidViewModel
import com.cpe211.greensight.db.AppDatabase
import com.cpe211.greensight.db.dao.HumidityDataDao
import com.cpe211.greensight.db.dao.TemperatureDataDao
import com.cpe211.greensight.model.HumidityData
import com.cpe211.greensight.model.TemperatureData
import okhttp3.*
import java.io.IOException
import java.util.*
import kotlinx.coroutines.*

class SharedViewModel(application: Application) : AndroidViewModel(application) {
    private val timer = Timer()
    private val temperatureDataDao: TemperatureDataDao
    private val humidityDataDao: HumidityDataDao
    private val timerScope = CoroutineScope(Dispatchers.Default)

    init {
        val database = AppDatabase.getDatabase(application.applicationContext)
        temperatureDataDao = database.temperatureDataDao()
        humidityDataDao = database.humidityDataDao()
    }

    suspend fun getTemperatureDataInRange(startTime: Long, endTime: Long): List<TemperatureData> {
        return temperatureDataDao.getTemperatureDataInRange(startTime, endTime)
    }

    suspend fun getHumidityDataInRange(startTime: Long, endTime: Long): List<HumidityData> {
        return humidityDataDao.getHumidityDataInRange(startTime, endTime)
    }

    suspend fun getAllTemperatureData(): List<TemperatureData> {
        return temperatureDataDao.getAllTemperatureData()
    }

    suspend fun getAllHumidityData(): List<HumidityData> {
        return humidityDataDao.getAllHumidityData()
    }

    fun getIPAddress(context: Context): String {
        val sharedPref = context.getSharedPreferences("ip_pref", Context.MODE_PRIVATE)
        return sharedPref.getString("ip_address", "") ?: ""
    }

    fun fetchTemperature(ipAddress: String, client: OkHttpClient, callback: (String?) -> Unit) {
        val url = "http://$ipAddress/temperature"
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val temperature = response.body?.string()
                    callback(temperature)
                } catch (e: Exception) {
                    callback(null)
                } finally {
                    response.close()
                }
            }
        })
    }

    fun fetchHumidity(ipAddress: String, client: OkHttpClient, callback: (String?) -> Unit) {
        val url = "http://$ipAddress/humidity"
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val humidity = response.body?.string()
                    callback(humidity)
                } catch (e: Exception) {
                    callback(null)
                } finally {
                    response.close()
                }
            }
        })
    }

    fun startUpdatingTemperatureAndHumidity(
        ipAddress: String,
        client: OkHttpClient,
        temperatureTextView: TextView,
        humidityTextView: TextView
    ) {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                fetchTemperature(ipAddress, client) { temperature ->
                    temperatureTextView.post {
                        val formattedTemp = if (temperature.isNullOrEmpty()) "N/A" else "$temperature °C"
                        temperatureTextView.text = formattedTemp
                    }
                }

                fetchHumidity(ipAddress, client) { humidity ->
                    humidityTextView.post {
                        val formattedHumidity = if (humidity.isNullOrEmpty()) "N/A" else "$humidity %"
                        humidityTextView.text = formattedHumidity
                    }
                }
            }
        }, 0, 10000)
    }

    fun getUserName(context: Context): String {
        val sharedPref = context.getSharedPreferences("username_pref", Context.MODE_PRIVATE)
        return sharedPref.getString("user_name", "") ?: ""
    }

    fun getTemperature(context: Context): Float {
        val sharedPref = context.getSharedPreferences("temperature_pref", Context.MODE_PRIVATE)
        return sharedPref.getFloat("temperature", -1.0f) // Default value is -1.0f
    }

    fun getHumidity(context: Context): Float {
        val sharedPref = context.getSharedPreferences("humidity_pref", Context.MODE_PRIVATE)
        return sharedPref.getFloat("humidity", -1.0f) // Default value is -1.0f
    }

    fun controlActuator(
        action: String,
        ipAddress: String,
        client: OkHttpClient,
        callback: (Boolean, String) -> Unit
    ) {
        val url = "http://$ipAddress/$action"
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, "Failed to perform action $action")
                call.cancel()
            }

            override fun onResponse(call: Call, response: Response) {
                val success = response.isSuccessful
                response.close()

                // Invoke the callback
                callback(success, if (success) "Action $action successful" else "Failed to perform action $action")
            }
        })
    }
    fun fetchAndSaveData(ipAddress: String, client: OkHttpClient) {
        val currentTime = Calendar.getInstance().timeInMillis

        fetchTemperature(ipAddress, client) { temperature ->
            temperature?.toDoubleOrNull()?.let {
                val temperatureData = TemperatureData(temperatureValue = it, timestamp = currentTime)
                saveTemperatureData(temperatureData)
            }
        }

        fetchHumidity(ipAddress, client) { humidity ->
            humidity?.toDoubleOrNull()?.let {
                val humidityData = HumidityData(humidityValue = it, timestamp = currentTime)
                saveHumidityData(humidityData)
            }
        }
    }

    private fun saveTemperatureData(temperatureData: TemperatureData) {
        viewModelScope.launch {
            temperatureDataDao.insert(temperatureData)
        }
    }

    private fun saveHumidityData(humidityData: HumidityData) {
        viewModelScope.launch {
            humidityDataDao.insert(humidityData)
        }
    }
    fun startPeriodicDataFetch(ipAddress: String, client: OkHttpClient) {
        timerScope.launch {
            while (isActive) {
                fetchAndSaveData(ipAddress, client)
                delay(10000) // Fetch data every 10 seconds
            }
        }
    }
}