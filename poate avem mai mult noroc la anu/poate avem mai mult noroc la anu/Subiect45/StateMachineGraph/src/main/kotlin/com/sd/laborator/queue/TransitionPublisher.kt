package com.sd.laborator.queue

import com.sd.laborator.STATE_QUEUE
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.RabbitClient

/**
 * TransitionPublisher - PRODUCATORUL: pune mesaje in coada RabbitMQ.
 *
 * @RabbitClient genereaza implementarea; @Binding = cheia de rutare (exchange-ul implicit "" duce
 * mesajul direct in coada cu acelasi nume). Mesajul e concatenat cu separator: "entryNode|transition".
 *
 * SOLID(S): singura responsabilitate = publicarea mesajelor in coada.
 */
@RabbitClient
interface TransitionPublisher {

    @Binding(STATE_QUEUE)
    fun publish(message: String)
}
