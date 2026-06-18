package com.sd.laborator.interfaces

/** Interfata dedicata operatiei DELETE. Respecta ISP. */
interface IDeleteService {
    fun deleteBeer(id: Int): Boolean
}
