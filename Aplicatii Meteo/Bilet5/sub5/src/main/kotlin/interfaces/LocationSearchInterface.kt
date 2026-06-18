package com.sd.laborator.interfaces

import com.sd.laborator.pojo.GeoLocation

interface LocationSearchInterface {
    fun getLocation(cityName: String): GeoLocation?
}