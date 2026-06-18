package com.sd.laborator.chain

import com.sd.laborator.pojo.WeatherChainContext

// Veriga 2 - MODEL FILTRU: filtreaza datele pe numele orasului si extrage min/max temperatura
class CityFilterHandler : AbstractWeatherChainHandler() {

    override fun handle(context: WeatherChainContext): WeatherChainContext {
        val filtered = context.rawData.filter { point ->
            point.city.equals(context.cityName, ignoreCase = true)
        }

        if (filtered.isEmpty()) {
            throw IllegalStateException("Nu exista date pentru orașul ${context.cityName}.")
        }

        val minTemp = filtered.minOf { it.temperature }
        val maxTemp = filtered.maxOf { it.temperature }

        return passToNext(context.copy(
            filteredData = filtered,
            minTemp = minTemp,
            maxTemp = maxTemp
        ))
    }
}
