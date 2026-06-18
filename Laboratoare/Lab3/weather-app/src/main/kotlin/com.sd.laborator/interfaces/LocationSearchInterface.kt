package com.sd.laborator.interfaces

// Clasa simpla care retine coordonatele unei locatii
data class LocationData(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

// Interfata pentru cautarea locatiei
interface LocationSearchInterface {

    // Returneaza latitudine si longitudine pe baza numelui
    fun getLocation(locationName: String): LocationData?
}