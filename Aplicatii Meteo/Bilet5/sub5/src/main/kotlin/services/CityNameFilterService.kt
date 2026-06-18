package com.sd.laborator.services

import com.sd.laborator.interfaces.WeatherDataFilter
import com.sd.laborator.pojo.WeatherPoint
import org.springframework.stereotype.Service

@Service
class CityNameFilterService : WeatherDataFilter {

    override fun apply(
        dataStream: List<WeatherPoint>,
        cityName: String
    ): List<WeatherPoint> {
        return dataStream.filter {
            it.city.equals(cityName, ignoreCase = true)
        }
    }
}