package com.sd.laborator.interfaces

import com.sd.laborator.pojo.GeoLocation

interface ILocationSearchService {
    fun getLocation(cityName: String): GeoLocation?
}
