package com.cpe211.greensight

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText

class SettingsFragment : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var buttonSeedling: Button
    private lateinit var buttonFlowering: Button
    private lateinit var buttonVegetative: Button
    private lateinit var buttonManual: Button
    private lateinit var buttonSubmitManual: Button
    private lateinit var textEditLowTemp: TextInputEditText
    private lateinit var textEditLowHum: TextInputEditText
    private lateinit var textEditHighTemp: TextInputEditText
    private lateinit var textEditHighHum: TextInputEditText
    private lateinit var textRangeInfo: TextView
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var lastSelectedPhase: String
    private var settingsRestored = false



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE)
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        // Find buttons in the inflated layout
        buttonSeedling = view.findViewById(R.id.button_switch_fan)
        buttonFlowering = view.findViewById(R.id.button_switch_roof)
        buttonVegetative = view.findViewById(R.id.button_switch_mist)
        buttonManual = view.findViewById(R.id.preset_manual)
        buttonSubmitManual = view.findViewById(R.id.button_submit_manual_mode)

        textEditLowTemp = view.findViewById(R.id.textedit_low_temp)
        textEditLowHum = view.findViewById(R.id.textedit_low_hum)
        textEditHighTemp = view.findViewById(R.id.textedit_high_temp)
        textEditHighHum = view.findViewById(R.id.textedit_high_hum)

        textRangeInfo = view.findViewById(R.id.text_range_info)


        // Set onClickListeners for buttons if needed
        buttonSeedling.setOnClickListener {
            toggleButton(buttonSeedling)
            animateButton(buttonSeedling)
            handleButtonClick("Seedling Phase", 18.0f, 24.0f, 60.0f, 70.0f)
            buttonSubmitManual.visibility = View.INVISIBLE
            deactivateOtherButtons(buttonSeedling)
            saveSelectedPhase("Seedling Phase")
        }

        buttonFlowering.setOnClickListener {
            toggleButton(buttonFlowering)
            animateButton(buttonFlowering)
            handleButtonClick("Flowering Phase", 18.0f, 26.0f, 40.0f, 50.0f)
            buttonSubmitManual.visibility = View.INVISIBLE
            deactivateOtherButtons(buttonFlowering)
            saveSelectedPhase("Flowering Phase")
        }

        buttonVegetative.setOnClickListener {
            toggleButton(buttonVegetative)
            animateButton(buttonVegetative)
            handleButtonClick("Vegetative Phase", 20.0f, 28.0f, 40.0f, 60.0f)
            buttonSubmitManual.visibility = View.INVISIBLE
            deactivateOtherButtons(buttonVegetative)
            saveSelectedPhase("Vegetative Phase")
        }

        buttonManual.setOnClickListener {
            toggleButton(buttonManual)
            animateButton(buttonManual)
            handleManualButtonClick()
            buttonSubmitManual.visibility = View.VISIBLE
            deactivateOtherButtons(buttonManual)
            saveSelectedPhase("Manual")
        }

        buttonSubmitManual.setOnClickListener {
            submitManualMode()
        }

        if (!settingsRestored) {
            restoreSettings()
            settingsRestored = true // Set the flag to true after calling restoreSettings
        }

        return view
    }
    private fun toggleButton(button: Button) {
        // Toggle the selected state of the button
        if (!button.isSelected) {
            // Toggle the selected state of the button only if it's not already selected
            button.isSelected = true
        }
    }

    private fun deactivateOtherButtons(clickedButton: Button) {
        // Deactivate all buttons except the clicked button
        val buttonsToDeactivate = listOf(buttonSeedling, buttonFlowering, buttonVegetative, buttonManual)
        buttonsToDeactivate.filter { it != clickedButton }.forEach { it.isSelected = false }
    }

    private fun saveSelectedPhase(phase: String) {
        sharedPreferences.edit().putString("selected_phase", phase).apply()
        lastSelectedPhase = phase
    }

    private fun handleButtonClick(phase: String, lowTemp: Float, highTemp: Float, lowHum: Float, highHum: Float) {
        // Update SharedPreferences with the selected phase and temperature/humidity ranges
        with(sharedPreferences.edit()) {
            putString("selected_phase", phase)
            putFloat("low_temp", lowTemp)
            putFloat("high_temp", highTemp)
            putFloat("low_hum", lowHum)
            putFloat("high_hum", highHum)
            apply()
        }

        // Set hints and editability for text fields
        setEditTextProperties(isEditable = false, hint = "$lowTemp°C", editText = textEditLowTemp)
        setEditTextProperties(isEditable = false, hint = "$lowHum%", editText = textEditLowHum)
        setEditTextProperties(isEditable = false, hint = "$highTemp°C", editText = textEditHighTemp)
        setEditTextProperties(isEditable = false, hint = "$highHum%", editText = textEditHighHum)
        sharedViewModel.updatePhaseSettings(phase, lowTemp, highTemp, lowHum, highHum)
        textRangeInfo.text = "Temperature: $lowTemp°C - $highTemp°C, Humidity: $lowHum% - $highHum%"
    }

    private fun handleManualButtonClick() {
        // Update SharedPreferences with the selected phase as "Manual"
        sharedPreferences.edit().apply {
            putString("selected_phase", "Manual")
            apply()
        }

        // Set hints and editability for text fields in manual mode
        setEditTextProperties(isEditable = true, hint = "Enter low temperature", editText = textEditLowTemp)
        setEditTextProperties(isEditable = true, hint = "Enter low humidity", editText = textEditLowHum)
        setEditTextProperties(isEditable = true, hint = "Enter high temperature", editText = textEditHighTemp)
        setEditTextProperties(isEditable = true, hint = "Enter high humidity", editText = textEditHighHum)
        sharedViewModel.updateManualMode()
        textRangeInfo.text = ""
    }

    private fun submitManualMode() {
        val lowTemp = textEditLowTemp.text.toString().toFloatOrNull() ?: return
        val highTemp = textEditHighTemp.text.toString().toFloatOrNull() ?: return
        val lowHum = textEditLowHum.text.toString().toFloatOrNull() ?: return
        val highHum = textEditHighHum.text.toString().toFloatOrNull() ?: return

        if (lowTemp !in -40.0f..100.0f || highTemp !in -40.0f..100.0f || lowHum !in 0.0f..100.0f || highHum !in 0.0f..100.0f) {
            showToast("Please enter values within the specified range (-40 to 100 for temperature, 0 to 100 for humidity")
            return
        }

        if (lowTemp >= highTemp || lowHum >= highHum) {
            showToast("Low temperature/humidity must be less than high temperature/humidity")
            return
        }

        // Update SharedPreferences with the entered values
        sharedPreferences.edit().apply {
            putFloat("low_temp", lowTemp)
            putFloat("high_temp", highTemp)
            putFloat("low_hum", lowHum)
            putFloat("high_hum", highHum)
            apply()
        }

        // Set the values in the EditText fields
        textEditLowTemp.setText("")
        textEditHighTemp.setText("")
        textEditLowHum.setText("")
        textEditHighHum.setText("")
        setEditTextProperties(isEditable = true, hint = "$lowTemp°C", editText = textEditLowTemp)
        setEditTextProperties(isEditable = true, hint = "$lowHum%", editText = textEditLowHum)
        setEditTextProperties(isEditable = true, hint = "$highTemp°C", editText = textEditHighTemp)
        setEditTextProperties(isEditable = true, hint = "$highHum%", editText = textEditHighHum)

        sharedViewModel.updatePhaseSettings("Manual", lowTemp, highTemp, lowHum, highHum)
        textRangeInfo.text = "Temperature: $lowTemp°C - $highTemp°C, Humidity: $lowHum% - $highHum%"

        showToast("Manual mode settings updated successfully")
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun setEditTextProperties(isEditable: Boolean, hint: String, editText: TextInputEditText) {
        editText.isFocusable = isEditable
        editText.isClickable = isEditable
        editText.isFocusableInTouchMode = isEditable
        editText.hint = hint
    }

    private fun restoreSettings() {
        if (!::lastSelectedPhase.isInitialized) {
            lastSelectedPhase = sharedPreferences.getString("selected_phase", "") ?: ""
        }
        if (lastSelectedPhase.isEmpty()) {
            buttonSeedling.performClick()
        } else {
            when (lastSelectedPhase) {
                "Seedling Phase" -> buttonSeedling.performClick()
                "Flowering Phase" -> buttonFlowering.performClick()
                "Vegetative Phase" -> buttonVegetative.performClick()
                "Manual" -> {
                    buttonManual.performClick()
                    buttonSubmitManual.visibility = View.VISIBLE
                    val lowTemp = sharedPreferences.getFloat("low_temp", 0.0f)
                    val highTemp = sharedPreferences.getFloat("high_temp", 0.0f)
                    val lowHum = sharedPreferences.getFloat("low_hum", 0.0f)
                    val highHum = sharedPreferences.getFloat("high_hum", 0.0f)
                    // Set the values in the EditText fields
                    textEditLowTemp.setText(lowTemp.toString())
                    textEditHighTemp.setText(highTemp.toString())
                    textEditLowHum.setText(lowHum.toString())
                    textEditHighHum.setText(highHum.toString())
                    textRangeInfo.text = "Temperature: $lowTemp°C - $highTemp°C, Humidity: $lowHum% - $highHum%"
                }
            }
        }
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
}
