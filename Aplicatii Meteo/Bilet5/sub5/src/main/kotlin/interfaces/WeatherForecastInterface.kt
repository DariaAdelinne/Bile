package com.sd.laborator.interfaces

import com.sd.laborator.pojo.GeoLocation
import com.sd.laborator.pojo.WeatherPoint

interface WeatherForecastInterface {
    fun getForecastData(location: GeoLocation): List<WeatherPoint>
}