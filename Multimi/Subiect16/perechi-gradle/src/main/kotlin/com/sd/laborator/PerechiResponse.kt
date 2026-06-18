package com.sd.laborator

import io.micronaut.core.annotation.Introspected

/**
 * Corpul raspunsului HTTP.
 *   - adtA     : cele 100 de valori din ADT-ul A
 *   - adtB     : cele 100 de valori din ADT-ul B
 *   - perechi  : lista de perechi (a, b) cu a*b == a + b*3
 *   - message  : mesaj descriptiv
 */
@Introspected
class PerechiResponse {
    private var message: String? = null
    private var adtA: List<Int>? = null
    private var adtB: List<Int>? = null
    private var perechi: List<Map<String, Int>>? = null

    fun getMessage(): String? = message
    fun setMessage(m: String?) { message = m }

    fun getAdtA(): List<Int>? = adtA
    fun setAdtA(v: List<Int>?) { adtA = v }

    fun getAdtB(): List<Int>? = adtB
    fun setAdtB(v: List<Int>?) { adtB = v }

    fun getPerechi(): List<Map<String, Int>>? = perechi
    fun setPerechi(v: List<Map<String, Int>>?) { perechi = v }
}
