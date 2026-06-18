package com.sd.laborator.queue

import com.sd.laborator.STATE_QUEUE
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import io.micronaut.rabbitmq.connect.ChannelPool
import jakarta.inject.Singleton

/**
 * QueueDeclarer - declara coada in broker la pornire (inainte ca functia serverless sa inceapa
 * sa consume). Foloseste ChannelPool-ul micronaut-rabbitmq ca sa faca queueDeclare.
 *
 * Astfel coada exista garantat, indiferent cine porneste primul (producator/consumator).
 */
@Singleton
class QueueDeclarer : BeanCreatedEventListener<ChannelPool> {

    override fun onCreated(event: BeanCreatedEvent<ChannelPool>): ChannelPool {
        val pool = event.bean
        val channel = pool.channel
        try {
            // durable=true, exclusive=false, autoDelete=false
            channel.queueDeclare(STATE_QUEUE, true, false, false, null)
            println("[QueueDeclarer] Coada '$STATE_QUEUE' declarata in RabbitMQ.")
        } finally {
            pool.returnChannel(channel)
        }
        return pool
    }
}
