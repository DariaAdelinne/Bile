package com.sd.laborator

import jakarta.inject.Singleton

/**
 * Serviciu care implementeaza verificarea matematica din enunt.
 *
 * Conditia: a * b == a + b * 3
 *
 * Rezolvare analitica (pentru context):
 *   a*b = a + 3*b
 *   a*b - a = 3*b
 *   a*(b-1) = 3*b
 *   a = 3*b / (b-1)   [cu b != 1]
 *
 * Deci pentru un b dat, a este unic determinat (daca este intreg).
 * Aceasta functie verifica perechea (a, b) din ADT-urile primite.
 */
@Singleton
class VerificareService {

    /**
     * Verifica daca perechea (a, b) satisface a * b == a + b * 3.
     * Aceasta este "functia" mentionata in enunt.
     *
     * @param a element din ADT-ul A
     * @param b element din ADT-ul B
     * @return true daca a * b == a + b * 3
     */
    fun verificaPereche(a: Int, b: Int): Boolean {
        return a * b == a + b * 3
    }

    /**
     * Cauta toate perechile (a, b) din ADT-urile date care satisfac conditia.
     * Perechile duplicate (acelasi (a,b)) sunt incluse o singura data.
     *
     * @param adtA ADT-ul A
     * @param adtB ADT-ul B
     * @return lista de map-uri {"a": ..., "b": ...} cu perechile gasite
     */
    fun gasestePerechile(
        adtA: List<Int>,
        adtB: List<Int>
    ): List<Map<String, Int>> {
        val rezultate = mutableListOf<Map<String, Int>>()
        val vazute = mutableSetOf<Pair<Int, Int>>()

        for (a in adtA) {
            for (b in adtB) {
                val pereche = Pair(a, b)
                if (verificaPereche(a, b) && pereche !in vazute) {
                    rezultate.add(mapOf("a" to a, "b" to b))
                    vazute.add(pereche)
                }
            }
        }

        return rezultate
    }
}
