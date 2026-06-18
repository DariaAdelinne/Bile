package com.sd.laborator.controller

import com.sd.laborator.graph.NodeGraphService
import com.sd.laborator.model.NodeResponse
import com.sd.laborator.model.TransitionRequest
import com.sd.laborator.queue.TransitionPublisher
import com.sd.laborator.store.ResultStore
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post

/**
 * StateMachineController - punctul de intrare REST (cerinta biletului).
 *
 *   POST /transition  - userul cere o tranzitie; controllerul o PUNE IN COADA (nu o proceseaza direct).
 *                       Procesarea o face functia serverless care asculta coada (asincron).
 *   GET  /results     - rezultatele produse de functia serverless (nodurile in care s-a ajuns).
 *   GET  /graph       - definitia grafului (pentru vizualizare).
 *
 * SOLID(S): responsabilitatea de "intrare" (HTTP + punere in coada) e separata de procesare.
 */
@Controller
class StateMachineController(
    private val publisher: TransitionPublisher,
    private val store: ResultStore,
    private val graph: NodeGraphService
) {

    @Post("/transition", consumes = [MediaType.APPLICATION_JSON])
    fun transition(@Body request: TransitionRequest): HttpResponse<Map<String, String>> {
        // concatenare cu separator si punere in coada
        val message = "${request.entryNode}|${request.transition}"
        publisher.publish(message)
        println("[REST] Pus in coada: '$message'")
        return HttpResponse.accepted<Map<String, String>>()
            .body(mapOf("status" to "queued", "message" to message))
    }

    @Get("/results")
    fun results(): List<NodeResponse> = store.all()

    @Get("/graph")
    fun graph(): Map<String, List<String>> = graph.describe()
}
