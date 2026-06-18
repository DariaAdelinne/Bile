package com.sd.laborator

import jakarta.inject.Singleton
import java.util.LinkedList
import kotlin.random.Random

/**
 * Serviciu singleton care:
 *  1. Generează aleator două ADT-uri A și B, fiecare cu câte 100 de valori
 *     întregi în intervalul [1, 200]. Valorile pot fi duplicate în cadrul
 *     aceluiași ADT (comportament realistic pentru un ADT generic).
 *  2. Calculează intersecția A ∩ B și o depune în ADT-ul C.
 *
 * ADT-ul este modelat ca LinkedList<Int> (lista înlănțuită), structura
 * standard de tip ADT din cursul de Structuri de Date.
 */
@Singleton
class IntersectieService {

    companion object {
        const val ADT_SIZE = 100        // număr de elemente per ADT
        const val VALOARE_MIN = 1       // limita inferioară a valorilor aleatoare
        const val VALOARE_MAX = 200     // limita superioară a valorilor aleatoare (inclusiv)
    }

    /**
     * Generează un ADT (LinkedList) cu [ADT_SIZE] valori aleatoare
     * în intervalul [VALOARE_MIN, VALOARE_MAX].
     */
    fun genereazaADT(): LinkedList<Int> {
        val adt = LinkedList<Int>()
        repeat(ADT_SIZE) {
            adt.add(Random.nextInt(VALOARE_MIN, VALOARE_MAX + 1))
        }
        return adt
    }

    /**
     * Calculează intersecția a ∩ b și returnează rezultatul ca ADT nou (C).
     *
     * Algoritm:
     *   - Se parcurge fiecare element din A.
     *   - Dacă elementul există și în B ȘI nu a fost deja adăugat în C
     *     (pentru a evita duplicatele în rezultat), se adaugă în C.
     *
     * Complexitate: O(n * m) unde n = |A|, m = |B|.
     */
    fun calculeazaIntersectie(a: LinkedList<Int>, b: LinkedList<Int>): LinkedList<Int> {
        val c = LinkedList<Int>()

        for (element in a) {
            // elementul trebuie sa fie in B si sa nu fi fost deja pus in C
            if (b.contains(element) && !c.contains(element)) {
                c.add(element)
            }
        }

        return c
    }
}
