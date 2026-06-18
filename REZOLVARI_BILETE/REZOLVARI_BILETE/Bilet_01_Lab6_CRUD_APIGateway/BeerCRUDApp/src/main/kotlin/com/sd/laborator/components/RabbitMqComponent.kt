package com.sd.laborator.components

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
class RabbitMqComponent {

    @Value("\${spring.rabbitmq.host}")
    private lateinit var host: String

    @Value("\${spring.rabbitmq.port}")
    private val port: Int = 0

    @Value("\${spring.rabbitmq.username}")
    private lateinit var username: String

    @Value("\${spring.rabbitmq.password}")
    private lateinit var password: String

    @Value("\${beerapp.rabbitmq.exchange}")
    private lateinit var exchange: String

    @Value("\${beerapp.rabbitmq.routingkey}")
    private lateinit var routingKey: String

    fun getExchange(): String = exchange
    fun getRoutingKey(): String = routingKey

    @Bean
    fun connectionFactory(): ConnectionFactory {
        val factory = CachingConnectionFactory()
        factory.host = host
        factory.username = username
        factory.setPassword(password)
        factory.port = port
        return factory
    }

    @Bean
    fun rabbitTemplate(): RabbitTemplate = RabbitTemplate(connectionFactory())
}
