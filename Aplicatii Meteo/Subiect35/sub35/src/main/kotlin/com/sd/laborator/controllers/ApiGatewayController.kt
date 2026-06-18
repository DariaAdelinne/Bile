package com.sd.laborator.controllers

import com.sd.laborator.interfaces.IApiGateway
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class ApiGatewayController(
    private val apiGateway: IApiGateway
) {

    @GetMapping("/weather/{city}")
    fun getWeather(@PathVariable city: String): ResponseEntity<Any> {
        return try {
            ResponseEntity.ok(apiGateway.getWeather(city))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(e.message)
        }
    }

    @GetMapping("/weather/{city}/visualize")
    fun getVisualization(@PathVariable city: String): ResponseEntity<Any> {
        return try {
            ResponseEntity.ok(apiGateway.getVisualization(city))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(e.message)
        }
    }

    @GetMapping("/weather/{city}/replicate")
    fun getReplicated(
        @PathVariable city: String,
        @RequestParam(defaultValue = "3") instances: Int
    ): ResponseEntity<Any> {
        return try {
            ResponseEntity.ok(apiGateway.getReplicated(city, instances))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(e.message)
        }
    }
}
