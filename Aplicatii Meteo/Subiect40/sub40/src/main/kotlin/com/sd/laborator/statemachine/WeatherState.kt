package com.sd.laborator.statemachine

enum class WeatherState {
    IDLE,       // stare initiala - asteapta cerere
    FETCHING,   // descarca datele meteo de la API
    COMPUTING,  // calculeaza coeficientii de regresie liniara
    DONE,       // rezultate gata
    ERROR       // eroare in oricare pas
}
