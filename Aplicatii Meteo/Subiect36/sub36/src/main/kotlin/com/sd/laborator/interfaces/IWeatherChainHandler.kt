package com.sd.laborator.interfaces

import com.sd.laborator.pojo.WeatherChainContext

// Interfata pentru fiecare veriga din lant
interface IWeatherChainHandler {
    fun setNext(handler: IWeatherChainHandler): IWeatherChainHandler
    fun handle(context: WeatherChainContext): WeatherChainContext
}
