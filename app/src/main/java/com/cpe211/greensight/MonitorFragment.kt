package com.cpe211.greensight

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class MonitorFragment : Fragment() {

    private lateinit var lineChart: LineChart

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_monitor, container, false)
        lineChart = view.findViewById(R.id.temp_line_chart)

        // Sample data points for temperature
        val temperatureData = ArrayList<Entry>()
        temperatureData.add(Entry(0f, 20f))
        temperatureData.add(Entry(1f, 22f))
        temperatureData.add(Entry(2f, 21f))
        temperatureData.add(Entry(3f, 25f))
        temperatureData.add(Entry(4f, 23f))

        // Create a dataset with the data points
        val dataSet = LineDataSet(temperatureData, "Temperature")

        // Customize the appearance of the dataset if needed
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.aGreen) // Set color
        dataSet.valueTextColor = ContextCompat.getColor(requireContext(), R.color.aBlack) // Set value text color

        // Create a LineData object and add the dataset to it
        val lineData = LineData(dataSet)
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        // Set LineData to the LineChart
        lineChart.data = lineData

        // Refresh the chart
        lineChart.invalidate()

        return view
    }
}
