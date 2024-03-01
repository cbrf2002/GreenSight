package com.cpe211.greensight

import android.os.AsyncTask
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class TemperatureReader(private val ipAddress: String, private val callback: (String) -> Unit) :
    AsyncTask<Void, Void, String>() {

    override fun doInBackground(vararg params: Void?): String {
        var result = ""
        var connection: HttpURLConnection? = null
        try {
            val url = URL("http://$ipAddress/temperature")
            connection = url.openConnection() as HttpURLConnection
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            result = reader.readLine()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }
        return result
    }

    override fun onPostExecute(result: String) {
        super.onPostExecute(result)
        callback(result)
    }
}
