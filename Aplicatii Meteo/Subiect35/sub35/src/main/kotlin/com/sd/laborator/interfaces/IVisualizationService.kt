package com.sd.laborator.interfaces

import com.sd.laborator.pojo.WeatherPoint

interface IVisualizationService {
    fun visualize(data: List<WeatherPoint>): List<String>
}
