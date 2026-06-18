package com.sd.laborator

import com.sd.laborator.services.IntersectionService
import io.micronaut.function.FunctionBean
import io.micronaut.function.executor.FunctionInitializer
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.util.function.Function

/**
 * Functia Micronaut care:
 *  1. Primeste un IntersectionRequest
 *  2. Genereaza ADT-urile A si B cu cate [size] valori aleatoare
 *  3. Calculeaza intersectia A ∩ B -> ADT C
 *  4. Returneaza IntersectionResponse cu A, B, C
 *
 * Respecta principiul S: singura responsabilitate este orchestrarea calculului.
 * Respecta principiul D: depinde de IntersectionService prin injectie.
 */
@FunctionBean("intersectie-adt")
class IntersectionFunction : FunctionInitializer(), Function<IntersectionRequest, IntersectionResponse> {

    @Inject
    private lateinit var intersectionService: IntersectionService

    private val log = LoggerFactory.getLogger(IntersectionFunction::class.java)

    override fun apply(request: IntersectionRequest): IntersectionResponse {
        val size = request.getSize()
        log.info("Generare ADT A si B cu cate $size valori aleatoare din [1, ${IntersectionService.RANDOM_RANGE}]...")

        // Generare A si B
        val setA = intersectionService.generateRandomSet(size)
        val setB = intersectionService.generateRandomSet(size)

        log.info("A generat: ${setA.size()} elemente")
        log.info("B generat: ${setB.size()} elemente")

        // Calcul intersectie -> C
        val setC = intersectionService.intersect(setA, setB)

        log.info("Intersectia C = A ∩ B: ${setC.size()} elemente comune")

        // Construire raspuns
        val response = IntersectionResponse()
        response.setSetA(setA.getAll())
        response.setSetB(setB.getAll())
        response.setSetC(setC.getAll())
        response.setMessage(
            "A(${setA.size()} elemente) ∩ B(${setB.size()} elemente) = C(${setC.size()} elemente comune)"
        )

        return response
    }
}

fun main(args: Array<String>) {
    val function = IntersectionFunction()
    function.run(args) { ctx -> function.apply(ctx.get(IntersectionRequest::class.java)) }
}
