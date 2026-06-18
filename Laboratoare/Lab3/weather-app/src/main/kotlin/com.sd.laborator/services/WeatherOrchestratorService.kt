package com.sd.laborator.services

import org.springframework.stereotype.Service

@Service
class WeatherOrchestratorService(
    private val geoBlacklistService: GeoBlacklistService,
    private val locationSearchService: LocationSearchService,
    private val weatherForecastService: WeatherForecastService,
    private val timeService: TimeService
) {

    // Acest serviciu coordoneaza intreaga logica
    // Controllerul doar il apeleaza
    fun orchestrate(location: String): Any {

        val requestTime = timeService.getCurrentTime()

        // Verificam daca locatia este interzisa
        if (geoBlacklistService.isBlocked(location)) {
            return mapOf(
                "timestamp" to requestTime,
                "message" to geoBlacklistService.buildBlockedMessage(location)
            )
        }

        // Cautam coordonatele locatiei
        val locationData = locationSearchService.getLocation(location)
            ?: return mapOf(
                "timestamp" to requestTime,
                "message" to "Location not found"
            )

        // Cerem datele meteo
        val forecast = weatherForecastService.getForecastData(
            locationData.name,
            locationData.latitude,
            locationData.longitude
        ) ?: return mapOf(
            "timestamp" to requestTime,
            "message" to "Could not retrieve forecast"
        )

        return forecast
    }
}