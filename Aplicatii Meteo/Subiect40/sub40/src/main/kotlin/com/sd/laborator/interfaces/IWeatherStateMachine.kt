package com.sd.laborator.interfaces

import com.sd.laborator.pojo.StateMachineRequest
import com.sd.laborator.pojo.StateMachineResponse

interface IWeatherStateMachine {
    fun process(request: StateMachineRequest): StateMachineResponse
}
