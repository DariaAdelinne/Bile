package com.sd.laborator.services

import com.sd.laborator.interfaces.INumberSet
import com.sd.laborator.model.NumberSet
import jakarta.inject.Singleton
import kotlin.random.Random

/**
 * Serviciu care:
 *  1. Genereaza doua ADT-uri A si B cu cate SET_SIZE valori aleatoare
 *     din intervalul [1, RANDOM_RANGE]
 *  2. Calculeaza intersectia A ∩ B si o depune intr-un ADT C
 *
 * Respecta principiul S din SOLID: singura responsabilitate este
 * generarea datelor si calculul intersectiei.
 *
 * Respecta principiul D din SOLID: lucreaza cu INumberSet (interfata),
 * nu cu NumberSet (implementarea concreta).
 */
@Singleton
class IntersectionService {

    companion object {
        const val SET_SIZE = 100
        const val RANDOM_RANGE = 200
    }

    /**
     * Genereaza un ADT cu [size] valori intregi aleatoare din [1, range].
     * Deoarece un Set nu admite duplicate, continua pana obtine [size] elemente unice.
     */
    fun generateRandomSet(size: Int = SET_SIZE, range: Int = RANDOM_RANGE): INumberSet {
        val numberSet: INumberSet = NumberSet()
        while (numberSet.size() < size) {
            numberSet.add(Random.nextInt(1, range + 1))
        }
        return numberSet
    }

    /**
     * Calculeaza intersectia a doua ADT-uri si returneaza un nou ADT C
     * ce contine elementele comune.
     */
    fun intersect(a: INumberSet, b: INumberSet): INumberSet {
        val c: INumberSet = NumberSet()
        for (value in a.getAll()) {
            if (b.contains(value)) {
                c.add(value)
            }
        }
        return c
    }
}
