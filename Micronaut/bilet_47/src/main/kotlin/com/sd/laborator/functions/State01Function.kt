package com.sd.laborator.functions

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sd.laborator.interfaces.IStateFunction
import com.sd.laborator.model.AutomatonContext
import com.sd.laborator.rest.ResultStore
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

/**
 * Functie serverless — Starea S01.
 *
 * Tabel de tranzitii:
 *   input = 0 → S10
 *   input = 1 → S11  (doi de 1 consecutivi detectati — output=1)
 *
 * SRP: singura responsabilitate este gestionarea starii S01.
 */
@Component
class State01Function(
    private val kafka: KafkaTemplate<String, String>,
    private val store: ResultStore
) : IStateFunction {

    private val mapper = jacksonObjectMapper()

    override fun stateName() = "S01"

    @KafkaListener(topics = [KafkaTopics.S01], groupId = "automaton-group")
    fun onMessage(message: String) = process(mapper.readValue(message, AutomatonContext::class.java))

    override fun process(ctx: AutomatonContext) {
        println("[State01] id=${ctx.id} index=${ctx.bitIndex}/${ctx.bits.length} bit=${ctx.nextBit()}")

        val bit = ctx.nextBit()
        if (bit == null) {
            store.save(ctx.finish(output = 0))
            println("[State01] id=${ctx.id} DONE — output=0")
            return
        }

        val (nextState, nextTopic) = if (bit == '0') "S10" to KafkaTopics.S10 else "S11" to KafkaTopics.S11
        kafka.send(nextTopic, mapper.writeValueAsString(ctx.transition(nextState)))
    }
}
