package com.sd.laborator.chain

import com.sd.laborator.pojo.WeatherChainContext

// Veriga 4 (finala): formateaza datele pentru vizualizare
class WeatherVisualizationHandler : AbstractWeatherChainHandler() {

    override fun handle(context: WeatherChainContext): WeatherChainContext {
        val visualized = context.filteredData.map { point ->
            "[${point.city}] ${point.time} -> ${point.temperature}°C"
        }

        val updatedContext = context.copy(visualizedData = visualized)
        return passToNext(updatedContext)
    }
}
