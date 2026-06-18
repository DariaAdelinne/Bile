package com.sd.laborator.interfaces

import com.sd.laborator.pojo.Cheltuiala

/**
 * Interfata pentru serviciul de gestiune a cheltuielilor.
 * Respecta ISP (Interface Segregation Principle) - contine doar
 * operatiile CRUD relevante pentru cheltuieli.
 * Respecta DIP (Dependency Inversion Principle) - controlerele
 * depind de aceasta abstractizare, nu de implementarea concreta.
 */
interface ICheltuieliService {
    fun getCheltuiala(id: Int): Cheltuiala?
    fun createCheltuiala(cheltuiala: Cheltuiala)
    fun updateCheltuiala(id: Int, cheltuiala: Cheltuiala)
    fun deleteCheltuiala(id: Int)
    fun getCheltuieli(categorie: String, platitDe: String): List<Cheltuiala>
    fun getTotalCategorie(categorie: String): Double
}
