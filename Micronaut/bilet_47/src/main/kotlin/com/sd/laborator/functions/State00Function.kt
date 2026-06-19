package com.sd.laborator.functions

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sd.laborator.interfaces.IStateFunction
import com.sd.laborator.model.AutomatonContext
import com.sd.laborator.rest.ResultStore
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

/**
 * Functie serverless — Starea S00 (starea initiala / reset).
 *
 * Tabel de tranzitii:
 *   input = 0 → S00 (ramane)
 *   input = 1 → S01
 *
 * Daca nu mai sunt biti, marcheaza procesarea ca finalizata (output=0).
 *
 * SRP: singura responsabilitate este gestionarea starii S00.
 */
@Component
class State00Function(
    private val kafka: KafkaTemplate<String, String>,
    private val store: ResultStore
) : IStateFunction {

    private val mapper = jacksonObjectMapper()

    override fun stateName() = "S00"

    @KafkaListener(topics = [KafkaTopics.S00], groupId = "automaton-group")
    fun onMessage(message: String) = process(mapper.readValue(message, AutomatonContext::class.java))

    override fun process(ctx: AutomatonContext) {
        println("[State00] id=${ctx.id} index=${ctx.bitIndex}/${ctx.bits.length} bit=${ctx.nextBit()}")

        val bit = ctx.nextBit()
        if (bit == null) {
            store.save(ctx.finish(output = 0))
            println("[State00] id=${ctx.id} DONE — output=0")
            return
        }

        val (nextState, nextTopic) = if (bit == '0') "S00" to KafkaTopics.S00 else "S01" to KafkaTopics.S01
        kafka.send(nextTopic, mapper.writeValueAsString(ctx.transition(nextState)))
    }
}
