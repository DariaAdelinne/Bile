package com.sd.laborator.statemachine

import com.sd.laborator.interfaces.ILinearRegressionService
import com.sd.laborator.interfaces.ILocationSearchService
import com.sd.laborator.interfaces.IWeatherFetchService
import com.sd.laborator.interfaces.IWeatherStateMachine
import com.sd.laborator.pojo.StateMachineRequest
import com.sd.laborator.pojo.StateMachineResponse
import org.springframework.stereotype.Service

@Service
class WeatherStateMachine(
    private val locationSearchService: ILocationSearchService,
    private val weatherFetchService: IWeatherFetchService,
    private val linearRegressionService: ILinearRegressionService
) : IWeatherStateMachine {

    override fun process(request: StateMachineRequest): StateMachineResponse {
        val ctx = WeatherStateMachineContext(
            cityName = request.city,
            hours = request.hours
        )
        val history = mutableListOf<String>()

        // IDLE → FETCHING
        history.add(ctx.currentState.name)
        ctx.currentState = WeatherState.FETCHING
        history.add(ctx.currentState.name)

        try {
            val location = locationSearchService.getLocation(ctx.cityName)
                ?: throw IllegalArgumentException("Orașul ${ctx.cityName} nu a fost găsit.")

            val (times, temps) = weatherFetchService.fetchTemperatures(location, ctx.hours)
            ctx.times = times
            ctx.temperatures = temps

            if (temps.isEmpty()) throw IllegalStateException("Nu s-au obținut date de temperatură.")

            // FETCHING → COMPUTING
            ctx.currentState = WeatherState.COMPUTING
            history.add(ctx.currentState.name)

            val (a, b) = linearRegressionService.compute(temps)
            ctx.slopeA = a
            ctx.interceptB = b

            // COMPUTING → DONE
            ctx.currentState = WeatherState.DONE
            history.add(ctx.currentState.name)

        } catch (e: Exception) {
            ctx.currentState = WeatherState.ERROR
            ctx.errorMessage = e.message
            history.add(ctx.currentState.name)
        }

        return StateMachineResponse(
            city = ctx.cityName,
            hours = ctx.hours,
            finalState = ctx.currentState.name,
            temperatures = ctx.temperatures,
            times = ctx.times,
            slopeA = ctx.slopeA,
            interceptB = ctx.interceptB,
            stateHistory = history,
            errorMessage = ctx.errorMessage
        )
    }
}
