package com.sd.laborator.controllers

import com.sd.laborator.services.LocationSearchService
import com.sd.laborator.services.WeatherChainingService
import com.sd.laborator.services.WeatherForecastService
import com.sd.laborator.services.WeatherOrchestratorService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

// Controllerul pentru partea meteo
@RestController
class WeatherAppController(
    private val locationService: LocationSearchService,
    private val weatherService: WeatherForecastService,
    private val weatherChainingService: WeatherChainingService,
    private val weatherOrchestratorService: WeatherOrchestratorService
) {

    // Varianta initiala, simpla
    @GetMapping("/getforecast/{location}")
    fun getForecast(@PathVariable location: String): Any {
        val locationData = locationService.getLocation(location)
            ?: return "Location not found"

        val forecast = weatherService.getForecastData(
            locationData.name,
            locationData.latitude,
            locationData.longitude
        ) ?: return "Error getting forecast"

        return forecast
    }

    // Varianta cu chaining
    @GetMapping("/getforecast-chain/{location}")
    fun getForecastChain(@PathVariable location: String): Any {
        return weatherChainingService.getForecastWithChaining(location)
    }

    // Varianta cu orchestration
    @GetMapping("/getforecast-orchestrated/{location}")
    fun getForecastOrchestrated(@PathVariable location: String): Any {
        return weatherOrchestratorService.orchestrate(location)
    }
}