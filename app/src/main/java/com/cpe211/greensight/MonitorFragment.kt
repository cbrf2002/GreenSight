package com.cpe211.greensight

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.*
import androidx.lifecycle.lifecycleScope
import com.cpe211.greensight.model.HumidityData
import com.cpe211.greensight.model.TemperatureData
import com.github.mikephil.charting.formatter.ValueFormatter
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class MonitorFragment : Fragment() {

    private lateinit var temperatureLineChart: LineChart
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var ipAddress: String
    private val client = OkHttpClient()
    //private var periodicJob: Job? = null

    companion object {
        private val INTERVAL_LAST_HOUR = TimeUnit.HOURS.toMillis(1)
        private val INTERVAL_LAST_24_HOURS = TimeUnit.DAYS.toMillis(1)
        private val INTERVAL_LAST_7_DAYS = TimeUnit.DAYS.toMillis(7)
        private val INTERVAL_LAST_30_DAYS = TimeUnit.DAYS.toMillis(30)
        private val INTERVAL_LAST_12_MONTHS = TimeUnit.DAYS.toMillis(365)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_monitor, container, false)
        temperatureLineChart = view.findViewById(R.id.temp_line_chart)
        ipAddress = getIPAddressFromSharedPreferences()

        sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]

        val tabLayout = view.findViewById<TabLayout>(R.id.graph_timeframe_tabs)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    updateChartForTab(it.position)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        sharedViewModel.startPeriodicDataFetch(ipAddress, client)

        updateChartForTab(0)

        return view
    }
    /*override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //startPeriodicDataFetching()
    }*/
    /*private fun startPeriodicDataFetching() {
        periodicJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                fetchDataAndDisplay() // Fetch and display data

                delay(10000) // Fetch data every 10 seconds
            }
        }
    }

    private fun stopPeriodicDataFetching() {
        periodicJob?.cancel() // Cancel the periodic job when the fragment is destroyed or paused
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPeriodicDataFetching() // Stop the periodic data fetching when the view is destroyed
    }*/

    private fun getIPAddressFromSharedPreferences(): String {
        // Example of retrieving ipAddress from SharedPreferences
        val sharedPref = requireActivity().getSharedPreferences("ip_pref", Context.MODE_PRIVATE)
        return sharedPref.getString("ip_address", "") ?: ""
    }
    private fun updateChartForTab(tabPosition: Int) {
        val currentTime = Calendar.getInstance().timeInMillis
        val intervals = listOf(
            INTERVAL_LAST_HOUR,
            INTERVAL_LAST_24_HOURS,
            INTERVAL_LAST_7_DAYS,
            INTERVAL_LAST_30_DAYS,
            INTERVAL_LAST_12_MONTHS
        )
        val startTime = currentTime - intervals.getOrElse(tabPosition) { 0 }

        lifecycleScope.launch {
            val temperatureData = sharedViewModel.getTemperatureDataInRange(startTime, currentTime)
            val humidityData = sharedViewModel.getHumidityDataInRange(startTime, currentTime)
            updateCharts(temperatureData, humidityData)
        }
    }

    private fun updateCharts(temperatureData: List<TemperatureData>, humidityData: List<HumidityData>) {
        updateTemperatureChart(temperatureData)
        updateHumidityChart(humidityData)
    }

    private fun updateTemperatureChart(temperatureData: List<TemperatureData>) {
        val temperatureEntries = mutableListOf<Entry>()

        temperatureData.forEachIndexed { index, data ->
            temperatureEntries.add(Entry(index.toFloat(), data.temperatureValue.toFloat()))
        }

        val temperatureDataSet = LineDataSet(temperatureEntries, "")
        temperatureDataSet.color = ContextCompat.getColor(requireContext(), R.color.aGreen)
        temperatureDataSet.setDrawCircles(false)
        //temperatureDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        val lineData = LineData(temperatureDataSet)
        temperatureLineChart.data = lineData

        // Customize x-axis labels
        val xAxis = temperatureLineChart.xAxis
        xAxis.valueFormatter = object : ValueFormatter() {
            private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

            override fun getFormattedValue(value: Float): String {
                val timestamp = temperatureData.getOrNull(value.toInt())?.timestamp ?: 0
                val date = Date(timestamp)
                return dateFormat.format(date)
            }
        }
        xAxis.labelCount = 4

        temperatureLineChart.legend.isEnabled = false
        temperatureLineChart.description.isEnabled = false
        temperatureLineChart.invalidate()
    }

    private fun updateHumidityChart(humidityData: List<HumidityData>) {
        val humidityEntries = mutableListOf<Entry>()

        humidityData.forEachIndexed { index, data ->
            humidityEntries.add(Entry(index.toFloat(), data.humidityValue.toFloat()))
        }

        val humidityDataSet = LineDataSet(humidityEntries, "")
        humidityDataSet.color = ContextCompat.getColor(requireContext(), R.color.aGreen)
        humidityDataSet.setDrawCircles(false)
        //humidityDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        val lineData = LineData(humidityDataSet)

        val humidityLineChart = requireView().findViewById<LineChart>(R.id.humidity_line_chart)
        humidityLineChart.data = lineData

        // Customize x-axis labels for humidity chart
        val xAxis = humidityLineChart.xAxis
        xAxis.valueFormatter = object : ValueFormatter() {
            private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

            override fun getFormattedValue(value: Float): String {
                val timestamp = humidityData.getOrNull(value.toInt())?.timestamp ?: 0
                val date = Date(timestamp)
                return dateFormat.format(date)
            }
        }
        xAxis.labelCount = 4

        humidityLineChart.legend.isEnabled = false
        humidityLineChart.description.isEnabled = false
        humidityLineChart.invalidate()
    }

    /*
    private fun fetchDataAndDisplay() {
        lifecycleScope.launch {
            val temperatureData = sharedViewModel.getAllTemperatureData()
            val humidityData = sharedViewModel.getAllHumidityData()
        }
    }

    private fun buildDisplayText(
        temperatureData: List<TemperatureData>,
        humidityData: List<HumidityData>
    ): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("Temperature Data:\n")
        for (data in temperatureData) {
            stringBuilder.append("${convertTimestampToString(data.timestamp)}: ${data.temperatureValue} °C\n")
        }

        stringBuilder.append("\nHumidity Data:\n")
        for (data in humidityData) {
            stringBuilder.append("${convertTimestampToString(data.timestamp)}: ${data.humidityValue} %\n")
        }

        return stringBuilder.toString()
    }

    private fun convertTimestampToString(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = Date(timestamp)
        return dateFormat.format(date)
    }
    */
}