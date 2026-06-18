package com.sd.merkle

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

// Clasa principala care porneste microserviciul
@SpringBootApplication
class MerkleMicroservice

// Functia main - punctul de intrare in aplicatie
fun main(args: Array<String>) {
    // Porneste aplicatia Spring Boot
    runApplication<MerkleMicroservice>(*args)
}