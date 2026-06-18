package com.sd.laborator.controllers

import com.sd.laborator.queue.StateQueue
import com.sd.laborator.statemachine.MachineState
import com.sd.laborator.statemachine.StateContext
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

data class SubmitRequest(val payload: String)

data class SubmitResponse(val id: String, val message: String)

data class StatusResponse(
    val id: String,
    val state: String,
    val result: String?,
    val errorMessage: String?,
    val history: List<String>
)

/**
 * Serviciul REST — introduce datele utilizatorului in coada.
 *
 * Principiul S (SRP): singura responsabilitate este receptia datelor
 *                     via HTTP si introducerea lor in StateQueue.
 *
 * Endpoint-uri:
 *   POST /statemachine/submit       — trimite date noi in coada
 *   GET  /statemachine/status/{id}  — interogheaza starea unui element
 *   GET  /statemachine/results      — toate rezultatele finale
 *   GET  /statemachine/queue-size   — dimensiunea cozii
 */
@RestController
@RequestMapping("/statemachine")
class StateMachineController(private val queue: StateQueue) {

    /**
     * Introduce datele utilizatorului in coada cu starea initiala SUBMITTED.
     * Fiecare functie serverless va prelua elementul si il va avansa prin stari.
     */
    @PostMapping("/submit")
    fun submit(@RequestBody request: SubmitRequest): ResponseEntity<SubmitResponse> {
        val id = UUID.randomUUID().toString().take(8)
        val ctx = StateContext(
            id = id,
            payload = request.payload,
            currentState = MachineState.SUBMITTED
        )
        queue.enqueue(ctx)
        return ResponseEntity.ok(
            SubmitResponse(id, "Date introduse in coada. Urmariti progresul cu GET /statemachine/status/$id")
        )
    }

    /**
     * Interogheaza starea unui element dupa id.
     * Elementele in stari terminale (COMPLETED/ERROR) sunt disponibile imediat.
     * Elementele inca in procesare returneaza 404 pana la finalizare.
     */
    @GetMapping("/status/{id}")
    fun status(@PathVariable id: String): ResponseEntity<StatusResponse> {
        val ctx = queue.getResult(id)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(
            StatusResponse(
                id = ctx.id,
                state = ctx.currentState.name,
                result = ctx.result,
                errorMessage = ctx.errorMessage,
                history = ctx.history
            )
        )
    }

    @GetMapping("/results")
    fun allResults(): ResponseEntity<Map<String, StatusResponse>> {
        val results = queue.getAllResults().mapValues { (_, ctx) ->
            StatusResponse(ctx.id, ctx.currentState.name, ctx.result, ctx.errorMessage, ctx.history)
        }
        return ResponseEntity.ok(results)
    }

    @GetMapping("/queue-size")
    fun queueSize(): ResponseEntity<Map<String, Int>> =
        ResponseEntity.ok(mapOf("queueSize" to queue.queueSize()))
}
