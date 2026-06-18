package com.sd.laborator.interfaces

import com.sd.laborator.models.Beer

/**
 * Interfata pentru API Gateway.
 * Respecta DIP: controller-ul depinde de aceasta abstractizare.
 * Respecta ISP: contine toate operatiile CRUD reunite intr-un singur punct de intrare.
 */
interface IGatewayService {
    fun initTable(): Any
    fun addBeer(beer: Beer): Any
    fun getAllBeers(): Any
    fun getBeerById(id: Int): Any
    fun getBeerByName(name: String): Any
    fun getBeersByMaxPrice(maxPrice: Float): Any
    fun updateBeer(id: Int, beer: Beer): Any
    fun deleteBeer(id: Int): Any
}
