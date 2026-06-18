package com.sd.laborator

import io.micronaut.core.annotation.Introspected

/**
 * Raspunsul functiei contine:
 *  - setA: elementele ADT-ului A (100 valori aleatoare)
 *  - setB: elementele ADT-ului B (100 valori aleatoare)
 *  - setC: intersectia A ∩ B
 *  - message: mesaj descriptiv
 */
@Introspected
class IntersectionResponse {
    private var setA: List<Int> = emptyList()
    private var setB: List<Int> = emptyList()
    private var setC: List<Int> = emptyList()
    private var message: String = ""

    fun getSetA(): List<Int> = setA
    fun setSetA(v: List<Int>) { setA = v }

    fun getSetB(): List<Int> = setB
    fun setSetB(v: List<Int>) { setB = v }

    fun getSetC(): List<Int> = setC
    fun setSetC(v: List<Int>) { setC = v }

    fun getMessage(): String = message
    fun setMessage(v: String) { message = v }
}
