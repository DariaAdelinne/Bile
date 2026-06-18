package com.sd.laborator

import io.micronaut.core.annotation.Introspected

/**
 * Corpul răspunsului HTTP.
 *   - adtA   : elementele din ADT-ul A (100 valori aleatoare)
 *   - adtB   : elementele din ADT-ul B (100 valori aleatoare)
 *   - adtC   : intersecția A ∩ B, depusă în ADT-ul C
 *   - message: mesaj descriptiv
 */
@Introspected
class IntersectieResponse {
    private var message: String? = null
    private var adtA: List<Int>? = null
    private var adtB: List<Int>? = null
    private var adtC: List<Int>? = null

    fun getMessage(): String? = message
    fun setMessage(message: String?) { this.message = message }

    fun getAdtA(): List<Int>? = adtA
    fun setAdtA(adtA: List<Int>?) { this.adtA = adtA }

    fun getAdtB(): List<Int>? = adtB
    fun setAdtB(adtB: List<Int>?) { this.adtB = adtB }

    fun getAdtC(): List<Int>? = adtC
    fun setAdtC(adtC: List<Int>?) { this.adtC = adtC }
}
