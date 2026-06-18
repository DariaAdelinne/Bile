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

    @GetMapping("/weather/{city}")
    fun processCity(@PathVariable city: String): ResponseEntity<Any> {
        return try {
            val result = apiGateway.process(city)
            ResponseEntity.ok(mapOf(
                "city" to result.cityName,
                "country" to (result.location?.country ?: "N/A"),
                "minTemp" to result.minTemp,
                "maxTemp" to result.maxTemp,
                "recordCount" to result.filteredData.size,
                "outputFile" to result.outputFilePath
            ))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(e.message)
        }
    }
}
