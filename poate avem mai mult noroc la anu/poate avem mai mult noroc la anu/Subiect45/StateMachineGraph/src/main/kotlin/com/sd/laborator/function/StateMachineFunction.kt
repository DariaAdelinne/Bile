package com.sd.laborator.function

import com.sd.laborator.STATE_QUEUE
import com.sd.laborator.graph.NodeGraphService
import com.sd.laborator.model.NodeRequest
import com.sd.laborator.store.ResultStore
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener

/**
 * StateMachineFunction - FUNCTIA SERVERLESS (cerinta biletului).
 *
 * Spre deosebire de functia din laborator (care asculta o ruta HTTP cu @Post), aceasta ASCULTA
 * coada RabbitMQ (@RabbitListener + @Queue). Cand primeste un mesaj:
 *   1. il destructureaza  ("entryNode|transition", separatorul fiind pus de producator);
 *   2. il impacheteaza intr-un NodeRequest  (paralela la EratosteneRequest);
 *   3. aplica logica de graf: graph.apply(NodeRequest)  (paralela la handler.apply);
 *   4. obtine NodeResponse (nodul destinatie sau "no se puede"); rezultatul se ia cu un getter
 *      si se afiseaza / se pastreaza in ResultStore.
 *
 * SOLID(S): singura responsabilitate = a reactiona la mesajele din coada si a delega logica grafului.
 * SOLID(D): depinde de abstractia NodeGraphService, nu de detalii.
 */
@RabbitListener
class StateMachineFunction(
    private val graph: NodeGraphService,
    private val store: ResultStore
) {
    @Queue(STATE_QUEUE)
    fun onMessage(message: String) {
        // 1. destructurarea mesajului scos din coada
        val parts = message.split("|", limit = 2)
        if (parts.size < 2) {
            println("[Functie] Mesaj invalid din coada: '$message'")
            return
        }
        val (entryNode, transition) = parts

        // 2. impachetare in NodeRequest
        val request = NodeRequest(entryNode.trim(), transition.trim())

        // 3. + 4. aplicarea logicii de graf -> NodeResponse
        val response = graph.apply(request)
        store.add(response)

        if (response.valid) {
            println("[Functie] '${request.entryNode}' --(${request.transition})--> '${response.destination}'")
        } else {
            println("[Functie] '${request.entryNode}' --(${request.transition})--> no se puede :)")
        }
    }
}
