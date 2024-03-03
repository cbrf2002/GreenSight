package com.cpe211.greensight

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
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
    private val temperatureDataDao: TemperatureDataDao
    private val humidityDataDao: HumidityDataDao
    private val timerScope = CoroutineScope(Dispatchers.Default)
    private val sharedPreferences: SharedPreferences = application.getSharedPreferences("Settings", Context.MODE_PRIVATE)
    private var temperatureLow: Int = 0
    private var temperatureHigh: Int = 0
    private var humidityLow: Int = 0
    private var humidityHigh: Int = 0
    private var temperatureInt: Int = 0
    private var humidityInt: Int = 0

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

    fun getIPAddress(context: Context): String {
        val sharedPref = context.getSharedPreferences("ip_pref", Context.MODE_PRIVATE)
        return sharedPref.getString("ip_address", "") ?: ""
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun fetchData(endpoint: String, ipAddress: String, client: OkHttpClient): String? =
        suspendCancellableCoroutine { continuation ->
            val url = "http://$ipAddress/$endpoint"
            val request = Request.Builder().url(url).build()
            val call = client.newCall(request)

            continuation.invokeOnCancellation {
                call.cancel()
            }

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resume(null) {
                        call.cancel()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val data = response.body?.string()
                        continuation.resume(data) {
                            response.close()
                        }
                    } catch (e: Exception) {
                        continuation.resume(null) {
                            response.close()
                        }
                    }
                }
            })
        }

    private suspend fun fetchTemperature(ipAddress: String, client: OkHttpClient): String? =
        fetchData("temperature", ipAddress, client)

    private suspend fun fetchHumidity(ipAddress: String, client: OkHttpClient): String? =
        fetchData("humidity", ipAddress, client)

    fun startUpdatingTemperatureAndHumidity(
        ipAddress: String,
        client: OkHttpClient,
        temperatureTextView: TextView,
        humidityTextView: TextView
    ) {
        viewModelScope.launch {
            try {
                val temperature = fetchTemperature(ipAddress, client)
                val humidity = fetchHumidity(ipAddress, client)

                temperatureInt = temperature?.toIntOrNull() ?: 0
                humidityInt = humidity?.toIntOrNull() ?: 0

                with(sharedPreferences.edit()) {
                    putInt("temperature_int", temperatureInt)
                    putInt("humidity_int", humidityInt)
                    apply()
                }

                // Update UI with temperature and humidity values
                temperatureTextView.post {
                    val formattedTemp = temperature?.let { "$it °C" } ?: "N/A"
                    temperatureTextView.text = formattedTemp
                }

                humidityTextView.post {
                    val formattedHumidity = humidity?.let { "$it %" } ?: "N/A"
                    humidityTextView.text = formattedHumidity
                }

                // Save temperature and humidity data
                temperature?.toDoubleOrNull()?.let {
                    saveTemperatureData(TemperatureData(temperatureValue = it, timestamp = System.currentTimeMillis()))
                }

                humidity?.toDoubleOrNull()?.let {
                    saveHumidityData(HumidityData(humidityValue = it, timestamp = System.currentTimeMillis()))
                }
            } catch (e: Exception) {
                // Handle exceptions
                e.printStackTrace()
            }

            // Delay for 10 seconds
            delay(10000)
        }
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
    private suspend fun fetchAndSaveData(ipAddress: String, client: OkHttpClient) {
        val currentTime = Calendar.getInstance().timeInMillis

        fetchTemperature(ipAddress, client)?.toDoubleOrNull()?.let {
            saveTemperatureData(TemperatureData(temperatureValue = it, timestamp = currentTime))
        }

        fetchHumidity(ipAddress, client)?.toDoubleOrNull()?.let {
            saveHumidityData(HumidityData(humidityValue = it, timestamp = currentTime))
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

    fun updatePhaseSettings(phase: String, lowTemp: Int, highTemp: Int, lowHum: Int, highHum: Int) {
        // Update SharedPreferences with the selected phase and temperature/humidity ranges
        with(sharedPreferences.edit()) {
            putString("selected_phase", phase)
            putInt("low_temp", lowTemp)
            putInt("high_temp", highTemp)
            putInt("low_hum", lowHum)
            putInt("high_hum", highHum)
            apply()
        }
        // Update the values in the ViewModel
        temperatureLow = lowTemp
        temperatureHigh = highTemp
        humidityLow = lowHum
        humidityHigh = highHum
    }

    fun updateManualMode() {
        // Update SharedPreferences with manual mode
        with(sharedPreferences.edit()) {
            putString("selected_phase", "Manual")
            apply()
        }
    }
    fun getTemperatureLow(): Int {
        return sharedPreferences.getInt("low_temp", 0)
    }

    fun getTemperatureHigh(): Int {
        return sharedPreferences.getInt("high_temp", 0)
    }

    fun getHumidityLow(): Int {
        return sharedPreferences.getInt("low_hum", 0)
    }

    fun getHumidityHigh(): Int {
        return sharedPreferences.getInt("high_hum", 0)
    }

    fun getTemperatureInt(): Int {
        return sharedPreferences.getInt("temperature_int",0)
    }
    fun getHumidityInt(): Int {
        return sharedPreferences.getInt("humidity_int",0)
    }
}