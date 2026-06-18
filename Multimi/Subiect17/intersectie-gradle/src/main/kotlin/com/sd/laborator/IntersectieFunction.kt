package com.sd.laborator

import io.micronaut.function.FunctionBean
import io.micronaut.function.executor.FunctionInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import jakarta.inject.Inject
import java.util.function.Function

/**
 * Funcția Micronaut principală.
 *
 * La fiecare invocare:
 *   1. Generează ADT-ul A cu 100 de valori aleatoare.
 *   2. Generează ADT-ul B cu 100 de valori aleatoare.
 *   3. Calculează intersecția A ∩ B și o depune în ADT-ul C.
 *   4. Returnează A, B și C în corpul răspunsului JSON.
 */
@FunctionBean("intersectie")
class IntersectieFunction : FunctionInitializer(), Function<IntersectieRequest, IntersectieResponse> {

    @Inject
    private lateinit var intersectieService: IntersectieService

    private val LOG: Logger = LoggerFactory.getLogger(IntersectieFunction::class.java)

    override fun apply(msg: IntersectieRequest): IntersectieResponse {
        LOG.info("Incepe generarea ADT-urilor A si B ...")

        // pasul 1 & 2 - generare ADT-uri A si B cu cate 100 de valori aleatoare
        val adtA = intersectieService.genereazaADT()
        val adtB = intersectieService.genereazaADT()

        LOG.info("ADT A: $adtA")
        LOG.info("ADT B: $adtB")

        // pasul 3 - calcul intersectie A ∩ B -> C
        LOG.info("Se calculeaza intersectia A ∩ B ...")
        val adtC = intersectieService.calculeazaIntersectie(adtA, adtB)

        LOG.info("ADT C (intersectie): $adtC")
        LOG.info("Calcul incheiat! Intersectia contine ${adtC.size} elemente distincte.")

        // pasul 4 - construire raspuns
        val response = IntersectieResponse()
        response.setAdtA(adtA.toList())
        response.setAdtB(adtB.toList())
        response.setAdtC(adtC.toList())
        response.setMessage(
            "Intersectia A ∩ B contine ${adtC.size} elemente distincte."
        )

        return response
    }
}

/**
 * Punct de intrare CLI: echo '{}' | java -jar function.jar
 */
fun main(args: Array<String>) {
    val function = IntersectieFunction()
    function.run(args, { context -> function.apply(context.get(IntersectieRequest::class.java)) })
}
