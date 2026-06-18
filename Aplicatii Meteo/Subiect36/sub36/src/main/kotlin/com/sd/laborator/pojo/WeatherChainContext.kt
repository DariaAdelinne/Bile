package com.sd.laborator.pojo

// Contextul care se propaga prin lantul de handlere
data class WeatherChainContext(
    val cityName: String,
    var location: GeoLocation? = null,
    var rawData: List<WeatherPoint> = emptyList(),
    var filteredData: List<WeatherPoint> = emptyList(),
    var visualizedData: List<String> = emptyList()
)
