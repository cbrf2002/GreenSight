package com.cpe211.greensight


import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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

    private lateinit var debugtext: TextView
    private lateinit var textRangeInfoDash: TextView
    private var temperature: Float = 0.0f
    private var humidity: Float = 0.0f

    private val client = OkHttpClient()
    private var isAutoMode: Boolean = true


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


        temperature = sharedViewModel.getTemperature(requireContext())
        humidity = sharedViewModel.getHumidity(requireContext())
        val temperatureInt = sharedViewModel.getTemperatureInt()
        val humidityInt = sharedViewModel.getHumidityInt()

        mistControl = MistControl(ipAddress)
        fanControl = FanControl(ipAddress)
        roofControl = RoofControl(ipAddress)

        buttonOperationMode.setOnClickListener {
            isAutoMode = !isAutoMode
            animateButton(buttonOperationMode)
            updateOperationModeUI()
            updateButtonState()
        }

        controlButtonsBasedOnThresholds(temperatureInt, humidityInt)

        updateOperationModeUI()
        updateButtonState()

        buttonClickers()
        sharedViewModel.startUpdatingTemperatureAndHumidity(
            ipAddress,
            client,
            temperatureTextView,
            humidityTextView
        )
    }

    private fun toggleButton(button: Button) {
        // Toggle the selected state of the button
        button.isSelected = !button.isSelected
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
        activity?.runOnUiThread {
            if (success) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
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

    private fun controlButtonsBasedOnThresholds(temperatureInt: Int, humidityInt: Int) {
        val lowTemp = sharedViewModel.getTemperatureLow()
        val highTemp = sharedViewModel.getTemperatureHigh()
        val lowHum = sharedViewModel.getHumidityLow()
        val highHum = sharedViewModel.getHumidityHigh()
        textRangeInfoDash.text =
            "Temperature: $lowTemp°C - $highTemp°C, Humidity: $lowHum% - $highHum%, CurTemp: $temperatureInt, CurHum: $humidityInt"

        if (isAutoMode) {
            // Auto mode logic
            if (temperatureInt < lowTemp) {
                Toast.makeText(requireContext(), "temp<lowtemp", Toast.LENGTH_SHORT).show()
                // Below low temperature threshold
                enableButtonsIfNeeded()
                buttonMist.performClick() // Activate mist
                buttonFan.performClick() // Activate fan
                buttonRoof.performClick() // Close roof
            } else if (temperatureInt > highTemp) {
                // Above high temperature threshold
                Toast.makeText(requireContext(), "temp>hightemp", Toast.LENGTH_SHORT).show()
                enableButtonsIfNeeded()
                buttonMist.performClick() // Activate mist
                buttonFan.performClick() // Activate fan
                buttonRoof.performClick() // Open roof
            } else {
                // Within temperature range
                if (humidityInt < lowHum) {
                    // Below low humidity threshold
                    enableButtonsIfNeeded()
                    Toast.makeText(requireContext(), "hum<lowhum", Toast.LENGTH_SHORT).show()
                    buttonMist.performClick() // Activate mist
                    buttonRoof.performClick() // Close roof
                } else if (humidityInt > highHum) {
                    // Above high humidity threshold
                    enableButtonsIfNeeded()
                    Toast.makeText(requireContext(), "hum>highhum", Toast.LENGTH_SHORT).show()
                    buttonRoof.performClick() // Open roof
                } else {
                    // Within humidity range
                    enableButtonsIfNeeded()
                    Toast.makeText(requireContext(), "optimal", Toast.LENGTH_SHORT).show()
                    buttonMist.performClick() // Deactivate mist
                    buttonRoof.performClick() // Close roof
                    buttonFan.performClick() // Deactivate fan
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
}