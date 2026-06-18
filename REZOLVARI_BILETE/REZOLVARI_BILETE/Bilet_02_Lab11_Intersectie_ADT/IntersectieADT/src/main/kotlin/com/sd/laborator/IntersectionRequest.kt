package com.sd.laborator

import io.micronaut.core.annotation.Introspected

/**
 * Cererea catre functie.
 * Nu necesita parametri - A si B sunt generate aleator.
 * Campul optional "size" permite specificarea dimensiunii multimilor (implicit 100).
 */
@Introspected
class IntersectionRequest {
    private var size: Int = 100

    fun getSize(): Int = size
    fun setSize(size: Int) { this.size = size }
}
