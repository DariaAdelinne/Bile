package com.sd.laborator.functions

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sd.laborator.interfaces.IStateFunction
import com.sd.laborator.model.AutomatonContext
import com.sd.laborator.rest.ResultStore
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

/**
 * Functie serverless — Starea S11 (stare acceptoare, output=1).
 *
 * Automatul a detectat doi de 1 consecutivi. Output=1.
 * Daca mai sunt biti, continua procesarea din S11:
 *   input = 0 → S00
 *   input = 1 → S11 (ramane, continua sa produca output=1)
 *
 * La terminarea tuturor bitilor, salveaza rezultatul cu output=1.
 *
 * SRP: singura responsabilitate este gestionarea starii S11 (output).
 */
@Component
class State11Function(
    private val kafka: KafkaTemplate<String, String>,
    private val store: ResultStore
) : IStateFunction {

    private val mapper = jacksonObjectMapper()

    override fun stateName() = "S11"

    @KafkaListener(topics = [KafkaTopics.S11], groupId = "automaton-group")
    fun onMessage(message: String) = process(mapper.readValue(message, AutomatonContext::class.java))

    override fun process(ctx: AutomatonContext) {
        println("[State11] id=${ctx.id} *** OUTPUT=1 *** index=${ctx.bitIndex}/${ctx.bits.length}")

        // Contextul cu output=1 — starea acceptoare atinsa
        val accepted = ctx.copy(output = 1)

        val bit = accepted.nextBit()
        if (bit == null) {
            store.save(accepted.finish(output = 1))
            println("[State11] id=${accepted.id} DONE — output=1")
            return
        }

        // Continua procesarea dupa atingerea starii acceptoare
        val (nextState, nextTopic) = if (bit == '0') "S00" to KafkaTopics.S00 else "S11" to KafkaTopics.S11
        kafka.send(nextTopic, mapper.writeValueAsString(accepted.transition(nextState)))
    }
}
