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
import java.util.*

class DashboardFragment : Fragment() {

    private lateinit var ipAddress: String
    private lateinit var username: String
    private lateinit var mistControl: MistControl
    private lateinit var fanControl: FanControl
    private lateinit var roofControl: RoofControl

    private lateinit var temperatureTextView: TextView
    private lateinit var humidityTextView: TextView

    private lateinit var buttonFan: Button
    private lateinit var buttonRoof: Button
    private lateinit var buttonMist: Button
    private lateinit var sharedViewModel: SharedViewModel

    private lateinit var debugtext: TextView
    private var temperature: Float = 0.0f
    private var humidity: Float = 0.0f

    private val client = OkHttpClient()
    private val timer = Timer()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize your views and other properties
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        username = sharedViewModel.getUserName(requireContext())
        ipAddress = sharedViewModel.getIPAddress(requireContext())

        temperatureTextView = view.findViewById(R.id.var_temperature)
        humidityTextView = view.findViewById(R.id.var_humidity)
        debugtext = view.findViewById(R.id.debugtext)
        buttonFan = view.findViewById(R.id.button_switch_fan)
        buttonRoof = view.findViewById(R.id.button_switch_roof)
        buttonMist = view.findViewById(R.id.button_switch_mist)

        val textViewUsername = view.findViewById<TextView>(R.id.text_Owner)
        textViewUsername.text = if (username.isEmpty()) "Greensight's Greenhouse" else "$username's Greenhouse"


        temperature = sharedViewModel.getTemperature(requireContext())
        humidity = sharedViewModel.getHumidity(requireContext())

        temperatureTextView.text = if (temperature == -1.0f) "N/A" else "$temperature °C"
        humidityTextView.text = if (humidity == -1.0f) "N/A" else "$humidity %"


        mistControl = MistControl(ipAddress)
        fanControl = FanControl(ipAddress)
        roofControl = RoofControl(ipAddress)

        buttonClickers()
        sharedViewModel.startUpdatingTemperatureAndHumidity(
            ipAddress,
            client,
            temperatureTextView,
            humidityTextView
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer.cancel()
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

    private fun buttonClickers() {
        buttonFan.setOnClickListener {
            toggleButton(buttonFan)
            animateButton(buttonFan)
            val action = if (buttonFan.isSelected) "fanOn" else "fanOff"
            sharedViewModel.controlActuator(action, ipAddress, client) { success, message ->
                activity?.runOnUiThread {
                    if (success) {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        buttonMist.setOnClickListener {
            toggleButton(buttonMist)
            animateButton(buttonMist)
            val action = if (buttonMist.isSelected) "mistOn" else "mistOff"
            sharedViewModel.controlActuator(action, ipAddress, client) { success, message ->
                activity?.runOnUiThread {
                    if (success) {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        buttonRoof.setOnClickListener {
            toggleButton(buttonRoof)
            animateButton(buttonRoof)
            val action = if (buttonRoof.isSelected) "roofOn" else "roofOff"
            sharedViewModel.controlActuator(action, ipAddress, client) { success, message ->
                activity?.runOnUiThread {
                    if (success) {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}