package com.sd.laborator.services

import com.sd.laborator.interfaces.ILocationSearchService
import com.sd.laborator.interfaces.IReplicationService
import com.sd.laborator.pojo.ReplicationResult
import org.springframework.stereotype.Service

@Service
class ReplicationService(
    private val locationSearchService: ILocationSearchService
) : IReplicationService {

    override fun replicate(cityName: String, instances: Int): List<ReplicationResult> {
        if (instances <= 0) throw IllegalArgumentException("Numărul de instanțe trebuie să fie pozitiv.")

        return (1..instances).map { index ->
            // fiecare instanta este un obiect nou de WeatherService si VisualizationService
            val weatherService = OpenMeteoWeatherService(locationSearchService)
            val visualizationService = WeatherVisualizationService()

            val weatherData = weatherService.getWeather(cityName)
            val visualized = visualizationService.visualize(weatherData)

            ReplicationResult(
                instanceIndex = index,
                city = cityName,
                visualizedData = visualized
            )
        }
    }
}
