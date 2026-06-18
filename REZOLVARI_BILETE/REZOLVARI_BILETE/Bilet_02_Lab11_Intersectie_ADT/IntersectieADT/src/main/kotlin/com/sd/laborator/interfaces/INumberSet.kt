package com.sd.laborator.interfaces

/**
 * Interfata ADT (Abstract Data Type) pentru o multime de numere intregi.
 *
 * Respecta principiul I din SOLID: interfata are o singura responsabilitate -
 * defineste contractul unui ADT de tip multime de numere.
 *
 * Respecta principiul D din SOLID: celelalte componente depind de
 * aceasta abstractizare, nu de implementarea concreta.
 */
interface INumberSet {
    /** Adauga un element in multime */
    fun add(value: Int)

    /** Verifica daca un element exista in multime */
    fun contains(value: Int): Boolean

    /** Returneaza toate elementele multimii */
    fun getAll(): List<Int>

    /** Returneaza numarul de elemente */
    fun size(): Int

    /** Goleste multimea */
    fun clear()
}
