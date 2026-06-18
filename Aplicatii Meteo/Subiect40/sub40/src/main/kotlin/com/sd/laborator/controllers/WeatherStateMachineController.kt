package com.sd.laborator.controllers

import com.sd.laborator.interfaces.IWeatherStateMachine
import com.sd.laborator.pojo.StateMachineRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/statemachine")
class WeatherStateMachineController(
    private val stateMachine: IWeatherStateMachine
) {

    @PostMapping("/process")
    fun process(@RequestBody request: StateMachineRequest): ResponseEntity<Any> {
        return try {
            ResponseEntity.ok(stateMachine.process(request))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(e.message)
        }
    }
}
