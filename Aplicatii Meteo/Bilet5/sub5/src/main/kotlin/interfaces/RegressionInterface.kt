package com.sd.laborator.interfaces

import com.sd.laborator.pojo.RegressionResult
import com.sd.laborator.pojo.WeatherPoint

interface RegressionInterface {
    fun calculate(data: List<WeatherPoint>, cityName: String): RegressionResult
}