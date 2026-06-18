package com.sd.laborator.statemachine

// Contextul intern al state machine-ului — se modifica la fiecare tranzitie
data class WeatherStateMachineContext(
    val cityName: String,
    val hours: Int,
    var currentState: WeatherState = WeatherState.IDLE,
    var temperatures: List<Double> = emptyList(),
    var times: List<String> = emptyList(),
    var slopeA: Double? = null,        // coeficientul a (panta) din y = a*x + b
    var interceptB: Double? = null,    // coeficientul b (interceptul) din y = a*x + b
    var errorMessage: String? = null
)
