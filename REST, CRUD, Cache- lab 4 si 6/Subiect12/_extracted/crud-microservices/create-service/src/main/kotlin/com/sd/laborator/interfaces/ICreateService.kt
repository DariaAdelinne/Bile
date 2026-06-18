package com.sd.laborator.interfaces

import com.sd.laborator.models.Beer

/** Interfata dedicata operatiei CREATE. Respecta ISP — o singura responsabilitate. */
interface ICreateService {
    fun initTable()
    fun addBeer(beer: Beer): Beer
}
