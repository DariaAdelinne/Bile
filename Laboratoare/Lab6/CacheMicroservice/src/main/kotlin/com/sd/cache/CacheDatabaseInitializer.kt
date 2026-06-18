package com.sd.cache

import com.sd.cache.business.interfaces.ICacheService
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

// Componenta Spring care se executa automat la pornirea aplicatiei
@Component
class CacheDatabaseInitializer(
    // Injectam serviciul de cache
    private val cacheService: ICacheService
) : CommandLineRunner {

    // Metoda run se executa automat dupa pornirea aplicatiei
    override fun run(vararg args: String?) {
        // Initializam baza de date
        // In cazul nostru, se creeaza tabela pentru cache daca nu exista
        cacheService.initDatabase()
    }
}