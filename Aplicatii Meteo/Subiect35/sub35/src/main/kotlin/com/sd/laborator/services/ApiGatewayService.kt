package com.sd.laborator.services

import com.sd.laborator.interfaces.IApiGateway
import com.sd.laborator.interfaces.ILocationSearchService
import com.sd.laborator.interfaces.IReplicationService
import com.sd.laborator.interfaces.IVisualizationService
import com.sd.laborator.interfaces.IWeatherService
import com.sd.laborator.pojo.ReplicationResult
import com.sd.laborator.pojo.WeatherPoint
import org.springframework.stereotype.Service

// API Gateway: unic punct de intrare — encapsuleaza toata aplicatia de laborator
@Service
class ApiGatewayService(
    private val locationSearchService: ILocationSearchService,
    private val replicationService: IReplicationService
) : IApiGateway {

    private val weatherService: IWeatherService = OpenMeteoWeatherService(locationSearchService)
    private val visualizationService: IVisualizationService = WeatherVisualizationService()

    override fun getWeather(cityName: String): List<WeatherPoint> {
        return weatherService.getWeather(cityName)
    }

    override fun getVisualization(cityName: String): List<String> {
        val data = weatherService.getWeather(cityName)
        return visualizationService.visualize(data)
    }

    override fun getReplicated(cityName: String, instances: Int): List<ReplicationResult> {
        return replicationService.replicate(cityName, instances)
    }
}
