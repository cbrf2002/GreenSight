package com.cpe211.greensight

import android.animation.ObjectAnimator
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.*
import okhttp3.OkHttpClient

class DashboardFragment : Fragment() {

    private lateinit var ipAddress: String
    private lateinit var username: String
    private lateinit var mistControl: MistControl
    private lateinit var fanControl: FanControl
    private lateinit var roofControl: RoofControl

    private lateinit var temperatureTextView: TextView
    private lateinit var humidityTextView: TextView

    private lateinit var buttonOperationMode: Button
    private lateinit var buttonFan: Button
    private lateinit var buttonRoof: Button
    private lateinit var buttonMist: Button
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var sharedPreferences: SharedPreferences


    private lateinit var debugtext: TextView
    private lateinit var textRangeInfoDash: TextView

    private val client = OkHttpClient()
    private var isAutoMode: Boolean = true

    private val refreshInterval = 5000L // 5 seconds
    private val coroutineScope = CoroutineScope(Dispatchers.Main)


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize your views and other properties
        sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]
        sharedPreferences = requireActivity().getSharedPreferences("dashboard_prefs", Context.MODE_PRIVATE)
        username = sharedViewModel.getUserName(requireContext())
        ipAddress = sharedViewModel.getIPAddress(requireContext())

        temperatureTextView = view.findViewById(R.id.var_temperature)
        humidityTextView = view.findViewById(R.id.var_humidity)
        debugtext = view.findViewById(R.id.debugtext)
        textRangeInfoDash = view.findViewById(R.id.text_range_info_dash)

        buttonOperationMode = view.findViewById(R.id.button_operation_mode)
        buttonFan = view.findViewById(R.id.button_switch_fan)
        buttonRoof = view.findViewById(R.id.button_switch_roof)
        buttonMist = view.findViewById(R.id.button_switch_mist)

        val textViewUsername = view.findViewById<TextView>(R.id.text_Owner)
        textViewUsername.text = if (username.isEmpty()) "Greensight's Greenhouse" else "$username's Greenhouse"

        mistControl = MistControl(ipAddress)
        fanControl = FanControl(ipAddress)
        roofControl = RoofControl(ipAddress)

        sharedViewModel.temperatureAndHumidityUpdate.observe(viewLifecycleOwner) {
            updateTemperatureAndHumidity(sharedViewModel.temperature, sharedViewModel.humidity)
        }

        buttonOperationMode.setOnClickListener {
            isAutoMode = !isAutoMode
            animateButton(buttonOperationMode)
            updateOperationModeUI()
            updateButtonState()
        }

        isAutoMode = sharedPreferences.getBoolean("is_auto_mode", false)
        updateOperationModeUI()
        updateButtonState()

        buttonClickers()

        sharedViewModel.startUpdatingTemperatureAndHumidity(
            ipAddress,
            client,
            temperatureTextView,
            humidityTextView
        )

        startDataRefreshCoroutine()
    }
    private fun startDataRefreshCoroutine() {
        coroutineScope.launch {
            while (isActive) {
                // Refresh data
                refreshData()

                // Delay for 5 seconds before the next refresh
                delay(refreshInterval)
            }
        }
    }

    private fun refreshData() {
        // Implement data refresh logic here
        sharedViewModel.startUpdatingTemperatureAndHumidity(
            ipAddress,
            client,
            temperatureTextView,
            humidityTextView
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cancel the coroutine when the view is destroyed
        coroutineScope.cancel()
    }


    private fun toggleButton(button: Button) {
        // Toggle the selected state of the button
        button.isSelected = !button.isSelected
    }

    private fun updateTemperatureAndHumidity(temperature: Float, humidity: Float) {
        // Update UI with temperature and humidity
        temperatureTextView.text = "$temperature °C"
        humidityTextView.text = "$humidity %"

        // Call the method to update button states based on thresholds
        controlButtonsBasedOnThresholds(temperature, humidity)
    }

    private fun animateButton(button: Button) {
        val scaleX = ObjectAnimator.ofFloat(button, View.SCALE_X, 0.95f)
        val scaleY = ObjectAnimator.ofFloat(button, View.SCALE_Y, 0.95f)

        scaleX.duration = 100
        scaleY.duration = 100

        scaleX.interpolator = AccelerateInterpolator()
        scaleY.interpolator = AccelerateInterpolator()

        scaleX.repeatCount = 1
        scaleY.repeatCount = 1

        scaleX.repeatMode = ObjectAnimator.REVERSE
        scaleY.repeatMode = ObjectAnimator.REVERSE

        scaleX.start()
        scaleY.start()
    }

    private fun updateOperationModeUI() {
        // Update the text of the button based on the operation mode
        if (isAutoMode) {
            buttonOperationMode.text = "AUTO"
            buttonOperationMode.isSelected = true // Set selected state
        } else {
            buttonOperationMode.text = "MANUAL"
            buttonOperationMode.isSelected = false // Clear selected state
        }
    }

    private fun updateButtonState() {
        // Enable or disable the other buttons based on the operation mode
        val isEnabled = !isAutoMode

        buttonFan.isEnabled = isEnabled
        buttonRoof.isEnabled = isEnabled
        buttonMist.isEnabled = isEnabled
    }

    private fun handleActionResponse(success: Boolean, message: String) {
        /*activity?.runOnUiThread {
            if (success) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }*/
    }
    private fun buttonClickers() {
        buttonFan.setOnClickListener {
            toggleButton(buttonFan)
            animateButton(buttonFan)
            val action = if (buttonFan.isSelected) "fanOn" else "fanOff"
            sharedViewModel.controlActuator(action, ipAddress, client) { success, message ->
                handleActionResponse(success, message)
            }
        }

        buttonMist.setOnClickListener {
            toggleButton(buttonMist)
            animateButton(buttonMist)
            val action = if (buttonMist.isSelected) "mistOn" else "mistOff"
            sharedViewModel.controlActuator(action, ipAddress, client) { success, message ->
                handleActionResponse(success, message)
            }
        }

        buttonRoof.setOnClickListener {
            toggleButton(buttonRoof)
            animateButton(buttonRoof)
            val action = if (buttonRoof.isSelected) "roofOn" else "roofOff"
            sharedViewModel.controlActuator(action, ipAddress, client) { success, message ->
                handleActionResponse(success, message)
            }
        }
    }

    private fun controlButtonsBasedOnThresholds(temperature: Float, humidity: Float) {
        val lowTemp = sharedViewModel.getTemperatureLow()
        val highTemp = sharedViewModel.getTemperatureHigh()
        val lowHum = sharedViewModel.getHumidityLow()
        val highHum = sharedViewModel.getHumidityHigh()
        textRangeInfoDash.text =
            "Temperature: $lowTemp°C - $highTemp°C, Humidity: $lowHum% - $highHum%, CurTemp: $temperature, CurHum: $humidity"

        if (isAutoMode) {
            // Auto mode logic
            when {
                humidity < highHum && temperature < lowTemp -> {
                    // Low Humidity / Low Temperature
                    buttonMist.isSelected = true
                    buttonFan.isSelected = false
                    buttonRoof.isSelected = false
                    sharedViewModel.controlActuator("mistOn", ipAddress, client) { success, message ->
                        handleActionResponse(success, message)
                    }
                    sharedViewModel.controlActuator("fanOff", ipAddress, client) { success, message ->
                        handleActionResponse(success, message)
                    }
                    sharedViewModel.controlActuator("roofOff", ipAddress, client) { success, message ->
                        handleActionResponse(success, message)
                    }
                }
                humidity < highHum && temperature >= highTemp -> {
                    // Low Humidity / High Temperature
                    buttonMist.isSelected = true
                    buttonFan.isSelected = true
                    buttonRoof.isSelected = false
                    sharedViewModel.controlActuator("mistOn", ipAddress, client) { success, message ->
                        handleActionResponse(success, message)
                    }
                    sharedViewModel.controlActuator("fanOn", ipAddress, client) { success, message ->
                        handleActionResponse(success, message)
                    }
                    sharedViewModel.controlActuator("roofOff", ipAddress, client) { success, message ->
                        handleActionResponse(success, message)
                    }
                }
                humidity >= highHum && temperature < lowTemp -> {
                    // High Humidity / Low Temperature
                    buttonMist.isSelected = false
                    buttonFan.isSelected = true
                    buttonRoof.isSelected = false
                    sharedViewModel.controlActuator("mistOff", ipAddress, client) { success, message ->
                        handleActionResponse(success, message)
                    }
                    sharedViewModel.controlActuator("fanOn", ipAddress, client) { success, message ->
                        handleActionResponse(success, message)
                    }
                    sharedViewModel.controlActuator("roofOff", ipAddress, client) { success, message ->
                        handleActionResponse(success, message)
                    }
                }
                humidity >= highHum && temperature >= highTemp -> {
                    // High Humidity / High Temperature
                    buttonMist.isSelected = false
                    buttonFan.isSelected = true
                    buttonRoof.isSelected = true
                    sharedViewModel.controlActuator("mistOff", ipAddress, client) { success, message ->
                        handleActionResponse(success, message)
                    }
                    sharedViewModel.controlActuator("fanOn", ipAddress, client) { success, message ->
                        handleActionResponse(success, message)
                    }
                    sharedViewModel.controlActuator("roofOn", ipAddress, client) { success, message ->
                        handleActionResponse(success, message)
                    }
                }
                else -> {
                    // Optimal Condition
                    buttonMist.isSelected = false
                    buttonFan.isSelected = false
                    buttonRoof.isSelected = false
                    sharedViewModel.controlActuator("mistOff", ipAddress, client) { success, message ->
                        handleActionResponse(success, message)
                    }
                    sharedViewModel.controlActuator("fanOff", ipAddress, client) { success, message ->
                        handleActionResponse(success, message)
                    }
                    sharedViewModel.controlActuator("roofOff", ipAddress, client) { success, message ->
                        handleActionResponse(success, message)
                    }
                }
            }
        }
    }

    private fun enableButtonsIfNeeded() {
        // Manually enable buttons if needed
        buttonFan.isEnabled = true
        buttonRoof.isEnabled = true
        buttonMist.isEnabled = true
    }
    override fun onStop() {
        super.onStop()
        // Save the state of the operation mode button
        sharedPreferences.edit().putBoolean("is_auto_mode", isAutoMode).apply()
    }
}