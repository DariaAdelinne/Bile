package com.sd.laborator.rest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sd.laborator.functions.KafkaTopics
import com.sd.laborator.model.AutomatonContext
import org.springframework.http.ResponseEntity
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.*
import java.util.UUID

data class SubmitRequest(val bits: String)

data class SubmitResponse(val id: String, val bits: String, val message: String)

data class ResultResponse(
    val id: String,
    val bits: String,
    val output: Int,
    val done: Boolean,
    val finalState: String,
    val history: List<String>
)

/**
 * Serviciu REST — introduce datele utilizatorului in Kafka.
 *
 * Endpoint-uri:
 *   POST /automaton/submit     — trimite un sir de biti spre procesare
 *   GET  /automaton/result/{id} — interogheaza rezultatul dupa ID
 *   GET  /automaton/results    — toate rezultatele disponibile
 *
 * Principii SOLID:
 *   S (SRP): singura responsabilitate este receptionarea datelor si publicarea in Kafka
 *   D (DIP): depinde de KafkaTemplate si ResultStore (abstractizari), nu de implementari
 *
 * Fluxul datelor:
 *   POST /submit → Kafka topic "automaton-s00" → State00Function → ... → State11Function → ResultStore
 */
@RestController
@RequestMapping("/automaton")
class AutomatonController(
    private val kafka: KafkaTemplate<String, String>,
    private val store: ResultStore
) {

    private val mapper = jacksonObjectMapper()

    /**
     * Introduce un sir de biti in Kafka pentru procesare de catre automat.
     * Bitii valizi sunt '0' si '1'. Exemplu: {"bits": "011011"}
     */
    @PostMapping("/submit")
    fun submit(@RequestBody request: SubmitRequest): ResponseEntity<SubmitResponse> {
        if (request.bits.any { it != '0' && it != '1' }) {
            return ResponseEntity.badRequest().build()
        }

        val id = UUID.randomUUID().toString().take(8)
        val ctx = AutomatonContext(id = id, bits = request.bits)

        // Publica in topic-ul starii initiale S00
        kafka.send(KafkaTopics.S00, mapper.writeValueAsString(ctx))

        println("[AutomatonController] Trimis id=$id bits='${request.bits}' in Kafka")

        return ResponseEntity.ok(
            SubmitResponse(id, request.bits, "Trimis in Kafka. Verifica rezultatul la GET /automaton/result/$id")
        )
    }

    /**
     * Interogheaza rezultatul unui sir de biti procesat.
     * Daca procesarea nu s-a terminat inca, returneaza 404.
     */
    @GetMapping("/result/{id}")
    fun result(@PathVariable id: String): ResponseEntity<ResultResponse> {
        val ctx = store.get(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(ctx.toResponse())
    }

    @GetMapping("/results")
    fun allResults(): ResponseEntity<List<ResultResponse>> =
        ResponseEntity.ok(store.getAll().values.map { it.toResponse() })

    private fun AutomatonContext.toResponse() = ResultResponse(
        id = id,
        bits = bits,
        output = output,
        done = done,
        finalState = currentState,
        history = history
    )
}
