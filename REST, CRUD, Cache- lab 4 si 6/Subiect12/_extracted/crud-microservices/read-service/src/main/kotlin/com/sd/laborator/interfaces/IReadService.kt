package com.sd.laborator.interfaces

import com.sd.laborator.models.Beer

/** Interfata dedicata operatiei READ. Respecta ISP. */
interface IReadService {
    fun getAll(): List<Beer>
    fun getById(id: Int): Beer?
    fun getByName(name: String): Beer?
    fun getByMaxPrice(maxPrice: Float): List<Beer>
}
