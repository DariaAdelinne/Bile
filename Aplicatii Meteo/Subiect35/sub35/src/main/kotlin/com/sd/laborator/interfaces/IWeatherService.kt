package com.sd.laborator.interfaces

import com.sd.laborator.pojo.WeatherPoint

interface IWeatherService {
    fun getWeather(cityName: String): List<WeatherPoint>
}
