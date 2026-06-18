package com.sd.laborator.services

import com.sd.laborator.interfaces.CityReaderInterface
import com.sd.laborator.interfaces.LocationSearchInterface
import com.sd.laborator.interfaces.RegressionInterface
import com.sd.laborator.interfaces.WeatherForecastInterface
import com.sd.laborator.pojo.RegressionResult
import com.sd.laborator.pojo.WeatherPoint
import org.springframework.stereotype.Service

@Service
class WeatherOrchestratorService(
    private val cityReaderService: CityReaderInterface,
    private val locationSearchService: LocationSearchInterface,
    private val weatherForecastService: WeatherForecastInterface,
    private val linearRegressionService: RegressionInterface
) {

    fun getAvailableCities(): List<String> {
        return cityReaderService.readCities()
    }

    fun getForecastForCity(cityName: String): List<WeatherPoint> {
        validateCityExistsInFile(cityName)

        val location = locationSearchService.getLocation(cityName)
            ?: throw IllegalArgumentException("Orașul $cityName nu a fost găsit prin API-ul de geocoding.")

        return weatherForecastService.getForecastData(location)
    }

    fun getRegressionForCity(cityName: String): RegressionResult {
        val forecastData = getForecastForCity(cityName)

        return linearRegressionService.calculate(
            data = forecastData,
            cityName = cityName
        )
    }

    private fun validateCityExistsInFile(cityName: String) {
        val cities = cityReaderService.readCities()

        val exists = cities.any {
            it.equals(cityName, ignoreCase = true)
        }

        if (!exists) {
            throw IllegalArgumentException(
                "Orașul $cityName nu există în fișierul cities.txt.txt."
            )
        }
    }
}