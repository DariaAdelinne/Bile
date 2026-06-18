package com.sd.laborator.chain

import com.sd.laborator.pojo.WeatherChainContext

// Veriga 3: filtreaza datele (pastreaza doar orele de zi: 06:00 - 21:00)
class WeatherFilterHandler : AbstractWeatherChainHandler() {

    override fun handle(context: WeatherChainContext): WeatherChainContext {
        val filtered = context.rawData.filter { point ->
            val hour = point.time.substringAfter("T").substringBefore(":").toIntOrNull() ?: 0
            hour in 6..21
        }

        val updatedContext = context.copy(filteredData = filtered)
        return passToNext(updatedContext)
    }
}
