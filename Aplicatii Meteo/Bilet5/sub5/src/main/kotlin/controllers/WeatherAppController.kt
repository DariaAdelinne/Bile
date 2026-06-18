package com.sd.laborator.controllers

import com.sd.laborator.services.WeatherOrchestratorService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/weather")
class WeatherAppController(
    private val weatherOrchestratorService: WeatherOrchestratorService
) {

    @GetMapping("/cities.txt")
    fun getCities(): ResponseEntity<Any> {
        return try {
            ResponseEntity.ok(weatherOrchestratorService.getAvailableCities())
        } catch (exception: Exception) {
            ResponseEntity.badRequest().body(exception.message)
        }
    }

    @GetMapping("/forecast/{city}")
    fun getForecast(@PathVariable city: String): ResponseEntity<Any> {
        return try {
            ResponseEntity.ok(weatherOrchestratorService.getForecastForCity(city))
        } catch (exception: Exception) {
            ResponseEntity.badRequest().body(exception.message)
        }
    }

    @GetMapping("/regression/{city}")
    fun getRegression(@PathVariable city: String): ResponseEntity<Any> {
        return try {
            ResponseEntity.ok(weatherOrchestratorService.getRegressionForCity(city))
        } catch (exception: Exception) {
            ResponseEntity.badRequest().body(exception.message)
        }
    }
}