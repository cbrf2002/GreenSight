package com.cpe211.greensight

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.widget.TextView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
    private var temperatureLow: Float = 0.0f
    private var temperatureHigh: Float = 0.0f
    private var humidityLow: Float = 0.0f
    private var humidityHigh: Float = 0.0f
    private var _temperature: Float = 0.0f
    val temperature: Float
        get() = _temperature

    private var _humidity: Float = 0.0f
    val humidity: Float
        get() = _humidity

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
                val temperatureString = fetchTemperature(ipAddress, client)
                val humidityString = fetchHumidity(ipAddress, client)

                // Update UI with temperature and humidity values
                temperatureTextView.post {
                    val formattedTemp = temperatureString?.let { "$it °C" } ?: "N/A"
                    temperatureTextView.text = formattedTemp
                }

                humidityTextView.post {
                    val formattedHumidity = humidityString?.let { "$it %" } ?: "N/A"
                    humidityTextView.text = formattedHumidity
                }

                // Save temperature and humidity data
                temperatureString?.toDoubleOrNull()?.let {
                    _temperature = it.toFloat()
                }

                humidityString?.toDoubleOrNull()?.let {
                    _humidity = it.toFloat()
                }

                // Notify observers about the changes in temperature and humidity
                notifyTemperatureAndHumidityUpdated()
            } catch (e: Exception) {
                // Handle exceptions
                e.printStackTrace()
            }

            // Delay for 10 seconds
            delay(10000)
        }
    }

    private val _temperatureAndHumidityUpdate = MutableLiveData<Unit>()
    val temperatureAndHumidityUpdate: LiveData<Unit>
        get() = _temperatureAndHumidityUpdate

    private fun notifyTemperatureAndHumidityUpdated() {
        _temperatureAndHumidityUpdate.value = Unit
    }

    fun getUserName(context: Context): String {
        val sharedPref = context.getSharedPreferences("username_pref", Context.MODE_PRIVATE)
        return sharedPref.getString("user_name", "") ?: ""
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

        fetchTemperature(ipAddress, client)?.toFloatOrNull()?.let {
            saveTemperatureData(TemperatureData(temperatureValue = it, timestamp = currentTime))
        }

        fetchHumidity(ipAddress, client)?.toFloatOrNull()?.let {
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
                delay(5000) // Fetch data every 10 seconds
            }
        }
    }

    fun updatePhaseSettings(phase: String, lowTemp: Float, highTemp: Float, lowHum: Float, highHum: Float) {
        // Update SharedPreferences with the selected phase and temperature/humidity ranges
        with(sharedPreferences.edit()) {
            putString("selected_phase", phase)
            putFloat("low_temp", lowTemp)
            putFloat("high_temp", highTemp)
            putFloat("low_hum", lowHum)
            putFloat("high_hum", highHum)
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
    fun getTemperatureLow(): Float {
        return sharedPreferences.getFloat("low_temp", 0.0f)
    }

    fun getTemperatureHigh(): Float {
        return sharedPreferences.getFloat("high_temp", 0.0f)
    }

    fun getHumidityLow(): Float {
        return sharedPreferences.getFloat("low_hum", 0.0f)
    }

    fun getHumidityHigh(): Float {
        return sharedPreferences.getFloat("high_hum", 0.0f)
    }
}