package com.sd.laborator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

// Clasa principala a aplicatiei Spring Boot
@SpringBootApplication
class LibraryApp

// Functia main - punctul de intrare in aplicatie
fun main(args: Array<String>) {
    // Porneste aplicatia Spring Boot
    runApplication<LibraryApp>(*args)
}