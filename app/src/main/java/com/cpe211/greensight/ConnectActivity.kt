package com.cpe211.greensight

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class ConnectActivity : AppCompatActivity() {

    companion object {
        const val IP_ADDRESS_KEY = "ip_address"
        const val USER_NAME_KEY = "user_name"
    }

    private lateinit var ipAddress: String
    private lateinit var username: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect)
        window.statusBarColor = ContextCompat.getColor(this, R.color.bgWhite)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.bgWhite)

        val nameEnterText = findViewById<EditText>(R.id.textfield_enter_name)
        val ipAddressEditText = findViewById<EditText>(R.id.textfield_enter_IP)
        val connectButton = findViewById<Button>(R.id.connect)
        val statusText = findViewById<TextView>(R.id.status_text)

        statusText.text = "2024.02.27"
        loadSavedData()

        connectButton.setOnClickListener {
            ipAddress = ipAddressEditText.text.toString()
            username = nameEnterText.text.toString()
            animateButton(connectButton)

            if (ipAddress.isEmpty()) {
                Toast.makeText(this, "Please enter an IP address", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show()
                // Save the IP address to SharedPreferences
                saveIPAddress(ipAddress)
                saveUserName(username)
                statusText.text = "Saving IP Address and Username..."

                // Check connection status of NodeMCU
                checkConnection(ipAddress)
                statusText.text = "Checking Connection."
            }
        }

        startActivity(intent)
    }

    private fun loadSavedData() {
        val sharedPref = getSharedPreferences("ip_pref", Context.MODE_PRIVATE)
        ipAddress = sharedPref.getString(IP_ADDRESS_KEY, "") ?: ""
        val ipAddressEditText = findViewById<EditText>(R.id.textfield_enter_IP)
        ipAddressEditText.setText(ipAddress)

        val usernameSharedPref = getSharedPreferences("username_pref", Context.MODE_PRIVATE)
        username = usernameSharedPref.getString(USER_NAME_KEY, "") ?: ""
        val nameEnterText = findViewById<EditText>(R.id.textfield_enter_name)
        nameEnterText.setText(username)
    }

    private fun saveIPAddress(ipAddress: String) {
        val sharedPref = getSharedPreferences("ip_pref", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(IP_ADDRESS_KEY, ipAddress)
            apply()
        }
    }

    private fun saveUserName(username: String) {
        val sharedPref = getSharedPreferences("username_pref", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(USER_NAME_KEY, username)
            apply()
        }
    }

    private fun checkConnection(ipAddress: String) {
        Thread {
            val pingSuccessful = pingIPAddress(ipAddress)
            runOnUiThread {
                if (ipAddress == "1.2.1.2") {
                    Toast.makeText(applicationContext, "Opening Dashboard [DEBUG]", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(applicationContext, MainActivity::class.java))
                    finish()
                } else if (pingSuccessful) {
                    Toast.makeText(applicationContext, "Connected to NodeMCU", Toast.LENGTH_SHORT).show()
                    // Proceed with further actions after successful connection
                    proceedAfterConnection()
                } else {
                    Toast.makeText(applicationContext, "Failed to connect to NodeMCU", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun proceedAfterConnection() {
        // Additional actions to take after successful connection, such as starting a new activity
        startActivity(Intent(applicationContext, MainActivity::class.java))
        finish()
    }

    private fun pingIPAddress(ipAddress: String): Boolean {
        val command = "/system/bin/ping -c 1 $ipAddress"
        var successCheck = false
        try {
            val process = Runtime.getRuntime().exec(command)
            val exitValue = process.waitFor()
            successCheck = exitValue == 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return successCheck
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