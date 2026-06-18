package com.sd.laborator.controllers

import com.sd.laborator.interfaces.IApiGateway
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class ApiGatewayController(
    private val apiGateway: IApiGateway
) {

    // returneaza toate datele din context (location, rawData, filteredData, visualizedData)
    @GetMapping("/weather/{city}")
    fun getWeather(@PathVariable city: String): ResponseEntity<Any> {
        return try {
            ResponseEntity.ok(apiGateway.process(city))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(e.message)
        }
    }

    // returneaza doar datele vizualizate (rezultatul final al lantului)
    @GetMapping("/weather/{city}/visualized")
    fun getVisualized(@PathVariable city: String): ResponseEntity<Any> {
        return try {
            val result = apiGateway.process(city)
            ResponseEntity.ok(result.visualizedData)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(e.message)
        }
    }
}
