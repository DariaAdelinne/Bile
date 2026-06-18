package com.sd.laborator.interfaces

import com.sd.laborator.pojo.WeatherForecastData

// Interfata pentru serviciul de vreme
interface WeatherForecastInterface {

    // Returneaza datele meteo
    fun getForecastData(
        locationName: String,
        latitude: Double,
        longitude: Double
    ): WeatherForecastData?
}