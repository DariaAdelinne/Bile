package com.sd.laborator.repository

import com.sd.laborator.models.Beer

/**
 * Interfata repository pentru Beer.
 * Respecta ISP: fiecare serviciu CRUD foloseste doar metodele de care are nevoie.
 * Respecta DIP: serviciile depind de aceasta abstractizare.
 */
interface IBeerRepository {
    fun initTable()
    fun add(beer: Beer)
    fun getAll(): List<Beer>
    fun getById(id: Int): Beer?
    fun getByName(name: String): Beer?
    fun update(beer: Beer)
    fun delete(id: Int)
}
