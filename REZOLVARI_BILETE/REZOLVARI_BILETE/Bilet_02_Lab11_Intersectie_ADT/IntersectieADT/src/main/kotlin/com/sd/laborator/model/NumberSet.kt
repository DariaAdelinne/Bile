package com.sd.laborator.model

import com.sd.laborator.interfaces.INumberSet

/**
 * Implementare concreta a ADT-ului INumberSet.
 *
 * Intern foloseste un LinkedHashSet pentru a mentine ordinea de inserare
 * si a asigura unicitatea elementelor.
 *
 * Respecta principiul L din SOLID: poate substitui complet INumberSet
 * fara a altera comportamentul asteptat.
 */
class NumberSet : INumberSet {

    private val data: MutableSet<Int> = LinkedHashSet()

    override fun add(value: Int) {
        data.add(value)
    }

    override fun contains(value: Int): Boolean {
        return data.contains(value)
    }

    override fun getAll(): List<Int> {
        return data.toList()
    }

    override fun size(): Int {
        return data.size
    }

    override fun clear() {
        data.clear()
    }

    override fun toString(): String {
        return "NumberSet$data"
    }
}
