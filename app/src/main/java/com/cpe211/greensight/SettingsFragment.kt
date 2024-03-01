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
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var buttonSeedling: Button
    private lateinit var buttonFlowering: Button
    private lateinit var buttonVegetative: Button
    private lateinit var buttonManual: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE)

        // Find buttons in the inflated layout
        buttonSeedling = view.findViewById(R.id.button_switch_fan)
        buttonFlowering = view.findViewById(R.id.button_switch_roof)
        buttonVegetative = view.findViewById(R.id.button_switch_mist)
        buttonManual = view.findViewById(R.id.preset_manual)

        // Set onClickListeners for buttons if needed
        buttonSeedling.setOnClickListener {
            toggleButton(buttonSeedling)
            animateButton(buttonSeedling)
            deactivateOtherButtons(buttonSeedling)
        }

        buttonFlowering.setOnClickListener {
            toggleButton(buttonFlowering)
            animateButton(buttonFlowering)
            deactivateOtherButtons(buttonFlowering)
        }

        buttonVegetative.setOnClickListener {
            toggleButton(buttonVegetative)
            animateButton(buttonVegetative)
            deactivateOtherButtons(buttonVegetative)
        }

        buttonManual.setOnClickListener {
            toggleButton(buttonManual)
            animateButton(buttonManual)
            deactivateOtherButtons(buttonManual)
        }

        return view
    }
    private fun toggleButton(button: Button) {
        // Toggle the selected state of the button
        button.isSelected = !button.isSelected
    }

    private fun deactivateOtherButtons(clickedButton: Button) {
        // Deactivate all buttons except the clicked button
        val buttonsToDeactivate = listOf(buttonSeedling, buttonFlowering, buttonVegetative, buttonManual)
        buttonsToDeactivate.filter { it != clickedButton }.forEach { it.isSelected = false }
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
