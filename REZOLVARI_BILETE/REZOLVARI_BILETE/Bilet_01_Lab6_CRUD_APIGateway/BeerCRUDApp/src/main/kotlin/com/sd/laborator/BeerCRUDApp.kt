package com.sd.laborator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
open class BeerCRUDApp

fun main(args: Array<String>) {
    runApplication<BeerCRUDApp>(*args)
}
