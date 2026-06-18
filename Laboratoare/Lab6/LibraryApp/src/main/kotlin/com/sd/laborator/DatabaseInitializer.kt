package com.sd.laborator

import com.sd.laborator.business.interfaces.ILibraryDAOService
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

// Componenta Spring care se executa la pornirea aplicatiei
@Component
class DatabaseInitializer(
    // Injectam serviciul care se ocupa de baza de date
    private val libraryDAOService: ILibraryDAOService
) : CommandLineRunner {

    // Aceasta metoda se executa automat la start
    override fun run(vararg args: String?) {
        // Initializam baza de date (creare tabela + date initiale)
        libraryDAOService.initDatabase()
    }
}