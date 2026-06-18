package com.sd.laborator.interfaces

import com.sd.laborator.pojo.GeoLocation

interface IWeatherFetchService {
    fun fetchTemperatures(location: GeoLocation, hours: Int): Pair<List<String>, List<Double>>
}
