package com.cpe211.greensight

import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class FanControl(private val ipAddress: String) {

    fun toggleFan(isOn: Boolean, callback: Callback) {
        val action = if (isOn) "On" else "Off"
        val url = "http://$ipAddress/fan$action"
        sendHttpRequest(url, callback)
    }

    private fun sendHttpRequest(url: String, callback: Callback) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()
        client.newCall(request).enqueue(callback)
    }
}
