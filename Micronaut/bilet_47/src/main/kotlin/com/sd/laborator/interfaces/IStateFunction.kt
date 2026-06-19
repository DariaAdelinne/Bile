package com.sd.laborator.interfaces

import com.sd.laborator.model.AutomatonContext

/**
 * Interfata pentru o functie serverless care gestioneaza o stare a automatului.
 *
 * Fiecare implementare:
 *   - asculta pe topic-ul Kafka corespunzator starii sale
 *   - citeste bitul curent din AutomatonContext
 *   - determina starea urmatoare conform tabelului de tranzitii
 *   - publica contextul actualizat pe topic-ul starii urmatoare
 *
 * Principii SOLID:
 *   I (ISP): interfata mica, dedicata exclusiv procesarii unei stari
 *   D (DIP): orchestratorul depinde de aceasta abstractizare
 *   O (OCP): adaugarea unei noi stari = nou @Component, fara modificari in altele
 */
interface IStateFunction {
    fun stateName(): String
    fun process(ctx: AutomatonContext)
}
