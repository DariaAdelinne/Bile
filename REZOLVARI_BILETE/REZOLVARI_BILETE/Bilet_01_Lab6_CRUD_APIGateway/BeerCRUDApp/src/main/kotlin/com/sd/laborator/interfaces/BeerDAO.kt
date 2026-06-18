package com.sd.laborator.interfaces

import com.sd.laborator.model.Beer

// Interfata DAO - respecta principiul I din SOLID (segregarea interfetelor)
// Fiecare operatie CRUD este declarata separat
interface BeerDAO {
    fun createBeerTable()
    fun addBeer(beer: Beer)
    fun getBeers(): String
    fun getBeerByName(name: String): String?
    fun getBeerByPrice(price: Float): String?
    fun updateBeer(beer: Beer)
    fun deleteBeer(name: String)
}
