package com.sd.laborator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * ErrorReportApplication - serviciul REST care primeste erorile de la procesorul de flux,
 * le adauga intr-un fisier XML local (errors.xml) si afiseaza in browser pagina completa
 * cu toate erorile + statisticile.
 *
 * Porneste pe http://localhost:8080
 */
@SpringBootApplication
open class ErrorReportApplication

fun main(args: Array<String>) {
    runApplication<ErrorReportApplication>(*args)
}
