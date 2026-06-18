package com.sd.cache

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

// Adnotare care marcheaza clasa principala a aplicatiei Spring Boot
@SpringBootApplication
class CacheMicroservice

// Functia main este punctul de pornire al aplicatiei
fun main(args: Array<String>) {
    // Porneste aplicatia Spring Boot
    runApplication<CacheMicroservice>(*args)
}