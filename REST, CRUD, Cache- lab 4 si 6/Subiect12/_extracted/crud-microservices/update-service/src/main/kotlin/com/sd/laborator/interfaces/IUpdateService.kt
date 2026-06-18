package com.sd.laborator.interfaces

import com.sd.laborator.models.Beer

/** Interfata dedicata operatiei UPDATE. Respecta ISP. */
interface IUpdateService {
    fun updateBeer(id: Int, beer: Beer): Boolean
}
