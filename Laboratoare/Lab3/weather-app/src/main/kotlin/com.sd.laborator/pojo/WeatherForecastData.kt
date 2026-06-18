package com.sd.laborator.pojo

// Clasa care tine toate datele despre vreme, doar transporta date
data class WeatherForecastData(
    var location: String,
    var date: String,
    var weatherState: String,
    var weatherStateIconURL: String,
    var windDirection: String,
    var windSpeed: Double,
    var minTemp: Double,
    var maxTemp: Double,
    var currentTemp: Double,
    var humidity: Int
)