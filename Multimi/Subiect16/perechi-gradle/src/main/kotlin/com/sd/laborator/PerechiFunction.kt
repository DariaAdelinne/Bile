package com.sd.laborator

import io.micronaut.function.FunctionBean
import io.micronaut.function.executor.FunctionInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import jakarta.inject.Inject
import java.util.function.Function

/**
 * Functia Micronaut principala (analogul EratosteneFunction).
 *
 * Flux:
 *   1. ADTService genereaza A si B cu cate 100 de valori aleatoare
 *      (aceasta parte corespunde "programului principal" din enunt).
 *   2. VerificareService cauta perechile (a, b) cu a*b == a + b*3
 *      (aceasta parte corespunde "functiei" din enunt).
 *   3. Rezultatele sunt returnate in JSON.
 */
@FunctionBean("perechi")
class PerechiFunction : FunctionInitializer(), Function<PerechiRequest, PerechiResponse> {

    @Inject
    private lateinit var adtService: ADTService

    @Inject
    private lateinit var verificareService: VerificareService

    private val LOG: Logger = LoggerFactory.getLogger(PerechiFunction::class.java)

    override fun apply(msg: PerechiRequest): PerechiResponse {
        // pasul 1 - programul principal: generare ADT-uri A si B
        LOG.info("Generare ADT-uri A si B ...")
        val adtA = adtService.genereazaADT()
        val adtB = adtService.genereazaADT()
        LOG.info("ADT A: $adtA")
        LOG.info("ADT B: $adtB")

        // pasul 2 - functia: cautare perechi (a,b) cu a*b == a + b*3
        LOG.info("Cautare perechi cu a*b == a + b*3 ...")
        val perechi = verificareService.gasestePerechile(adtA, adtB)

        // pasul 3 - constructie raspuns
        val mesaj = if (perechi.isEmpty()) {
            "Nu s-au gasit perechi care satisfac a*b == a + b*3."
        } else {
            "S-au gasit ${perechi.size} pereche(i) distincte care satisfac a*b == a + b*3."
        }

        LOG.info(mesaj)
        LOG.info("Perechi: $perechi")

        val response = PerechiResponse()
        response.setAdtA(adtA.toList())
        response.setAdtB(adtB.toList())
        response.setPerechi(perechi)
        response.setMessage(mesaj)

        return response
    }
}

/**
 * Punct de intrare CLI: echo '{}' | java -jar function.jar
 */
fun main(args: Array<String>) {
    val function = PerechiFunction()
    function.run(args, { context -> function.apply(context.get(PerechiRequest::class.java)) })
}
