package com.sd.laborator.services

import com.sd.laborator.interfaces.ICheltuieliService
import com.sd.laborator.pojo.Cheltuiala
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementarea serviciului de gestiune a cheltuielilor.
 *
 * Respecta SRP: se ocupa DOAR de logica de business pentru cheltuieli.
 * Respecta OCP: poate fi extinsa (ex: CheltuieliServiceCuBD) fara a fi modificata.
 * Respecta LSP: poate inlocui orice alta implementare a ICheltuieliService.
 */
@Service
class CheltuieliService : ICheltuieliService {

    companion object {
        // Date initiale pentru demonstratie
        val dateInitiale = arrayOf(
            Cheltuiala(1, "Factura curent", 250.0, "utilitati", "Tata"),
            Cheltuiala(2, "Cumparaturi supermarket", 430.5, "alimentare", "Mama"),
            Cheltuiala(3, "Abonament internet", 60.0, "utilitati", "Tata"),
            Cheltuiala(4, "Medicamente", 120.0, "sanatate", "Mama"),
            Cheltuiala(5, "Combustibil masina", 300.0, "transport", "Tata")
        )
    }

    // ConcurrentHashMap pentru thread-safety (mai multe replici pot accesa)
    private val cheltuieli = ConcurrentHashMap<Int, Cheltuiala>(
        dateInitiale.associateBy { it.id }
    )

    override fun getCheltuiala(id: Int): Cheltuiala? = cheltuieli[id]

    override fun createCheltuiala(cheltuiala: Cheltuiala) {
        cheltuieli[cheltuiala.id] = cheltuiala
    }

    override fun updateCheltuiala(id: Int, cheltuiala: Cheltuiala) {
        cheltuieli.remove(id)
        cheltuieli[cheltuiala.id] = cheltuiala
    }

    override fun deleteCheltuiala(id: Int) {
        cheltuieli.remove(id)
    }

    override fun getCheltuieli(categorie: String, platitDe: String): List<Cheltuiala> {
        return cheltuieli.values.filter { c ->
            c.categorie.contains(categorie, ignoreCase = true) &&
            c.platitDe.contains(platitDe, ignoreCase = true)
        }.toList()
    }

    override fun getTotalCategorie(categorie: String): Double {
        return cheltuieli.values
            .filter { it.categorie.equals(categorie, ignoreCase = true) }
            .sumOf { it.suma }
    }
}
