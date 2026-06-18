package com.sd.laborator.services

import com.sd.laborator.interfaces.RegressionInterface
import com.sd.laborator.interfaces.WeatherDataFilter
import com.sd.laborator.pojo.RegressionResult
import com.sd.laborator.pojo.WeatherPoint
import org.springframework.stereotype.Service
import kotlin.math.PI
import kotlin.math.atan

@Service
class LinearRegressionService(
    private val cityNameFilterService: WeatherDataFilter
) : RegressionInterface {

    override fun calculate(
        data: List<WeatherPoint>,
        cityName: String
    ): RegressionResult {

        val filteredData = cityNameFilterService.apply(data, cityName)

        if (filteredData.size < 2) {
            return RegressionResult(
                city = cityName,
                slope = 0.0,
                angleDegrees = 0.0,
                numberOfPoints = filteredData.size,
                message = "Nu există suficiente date pentru regresie liniară."
            )
        }

        val xValues = filteredData.indices.map { it.toDouble() }
        val yValues = filteredData.map { it.temperature }

        val meanX = xValues.average()
        val meanY = yValues.average()

        var numerator = 0.0
        var denominator = 0.0

        for (i in filteredData.indices) {
            numerator += (xValues[i] - meanX) * (yValues[i] - meanY)
            denominator += (xValues[i] - meanX) * (xValues[i] - meanX)
        }

        val slope = numerator / denominator
        val angleDegrees = atan(slope) * 180.0 / PI

        return RegressionResult(
            city = cityName,
            slope = slope,
            angleDegrees = angleDegrees,
            numberOfPoints = filteredData.size,
            message = "Unghiul dreptei de regresie pentru temperatura orașului $cityName este $angleDegrees grade."
        )
    }
}