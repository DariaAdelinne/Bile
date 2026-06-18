package com.sd.laborator.interfaces

import com.sd.laborator.pojo.WeatherChainContext

interface IApiGateway {
    fun process(cityName: String): WeatherChainContext
}
