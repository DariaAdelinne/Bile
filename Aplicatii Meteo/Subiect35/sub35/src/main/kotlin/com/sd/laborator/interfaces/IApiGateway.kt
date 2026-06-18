package com.sd.laborator.interfaces

import com.sd.laborator.pojo.ReplicationResult
import com.sd.laborator.pojo.WeatherPoint

interface IApiGateway {
    fun getWeather(cityName: String): List<WeatherPoint>
    fun getVisualization(cityName: String): List<String>
    fun getReplicated(cityName: String, instances: Int): List<ReplicationResult>
}
