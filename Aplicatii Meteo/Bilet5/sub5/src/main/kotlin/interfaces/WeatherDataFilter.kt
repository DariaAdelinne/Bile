package com.sd.laborator.interfaces

import com.sd.laborator.pojo.WeatherPoint

interface WeatherDataFilter {
    fun apply(dataStream: List<WeatherPoint>, cityName: String): List<WeatherPoint>
}