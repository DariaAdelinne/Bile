package com.sd.laborator.chain

import com.sd.laborator.interfaces.IWeatherChainHandler
import com.sd.laborator.pojo.WeatherChainContext

// Clasa de baza: gestioneaza legatura catre urmatorul handler
abstract class AbstractWeatherChainHandler : IWeatherChainHandler {

    private var next: IWeatherChainHandler? = null

    override fun setNext(handler: IWeatherChainHandler): IWeatherChainHandler {
        next = handler
        return handler
    }

    protected fun passToNext(context: WeatherChainContext): WeatherChainContext {
        return next?.handle(context) ?: context
    }
}
