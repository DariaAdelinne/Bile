package com.sd.laborator.services

import com.sd.laborator.interfaces.IVisualizationService
import com.sd.laborator.pojo.WeatherPoint

class WeatherVisualizationService : IVisualizationService {

    override fun visualize(data: List<WeatherPoint>): List<String> {
        return data.map { point ->
            "[${point.city}] ${point.time} -> ${point.temperature}°C"
        }
    }
}
