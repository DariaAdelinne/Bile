package com.sd.laborator.pojo

data class StateMachineResponse(
    val city: String,
    val hours: Int,
    val finalState: String,
    val temperatures: List<Double>,
    val times: List<String>,
    val slopeA: Double?,        // panta dreptei: y = a*x + b
    val interceptB: Double?,    // interceptul dreptei
    val stateHistory: List<String>,
    val errorMessage: String? = null
)
