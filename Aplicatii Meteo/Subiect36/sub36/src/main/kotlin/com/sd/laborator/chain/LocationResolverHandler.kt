package com.sd.laborator.chain

import com.sd.laborator.interfaces.ILocationSearchService
import com.sd.laborator.pojo.WeatherChainContext

// Veriga 1: rezolva numele orasului in coordonate geografice
class LocationResolverHandler(
    private val locationSearchService: ILocationSearchService
) : AbstractWeatherChainHandler() {

    override fun handle(context: WeatherChainContext): WeatherChainContext {
        val location = locationSearchService.getLocation(context.cityName)
            ?: throw IllegalArgumentException("Orașul ${context.cityName} nu a fost găsit.")

        val updatedContext = context.copy(location = location)
        return passToNext(updatedContext)
    }
}
