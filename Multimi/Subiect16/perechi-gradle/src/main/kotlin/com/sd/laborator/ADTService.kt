package com.sd.laborator

import jakarta.inject.Singleton
import java.util.LinkedList
import kotlin.random.Random

/**
 * Serviciu responsabil cu generarea ADT-urilor A si B.
 * Aceasta logica corespunde "programului principal" din enunt:
 * initializarea aleatoare a celor doua ADT-uri cu cate 100 de valori.
 *
 * ADT-ul este modelat ca LinkedList<Int>.
 */
@Singleton
class ADTService {

    companion object {
        const val ADT_SIZE = 100
        const val VALOARE_MIN = 1
        const val VALOARE_MAX = 50   // interval mic pentru sanse mai mari de solutii
    }

    /**
     * Genereaza un ADT (LinkedList) cu ADT_SIZE valori aleatoare
     * in intervalul [VALOARE_MIN, VALOARE_MAX].
     */
    fun genereazaADT(): LinkedList<Int> {
        val adt = LinkedList<Int>()
        repeat(ADT_SIZE) {
            adt.add(Random.nextInt(VALOARE_MIN, VALOARE_MAX + 1))
        }
        return adt
    }
}
