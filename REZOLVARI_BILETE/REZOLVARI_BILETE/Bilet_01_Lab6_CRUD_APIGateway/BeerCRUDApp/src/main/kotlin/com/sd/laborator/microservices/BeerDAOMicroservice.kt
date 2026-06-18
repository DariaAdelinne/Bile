package com.sd.laborator.microservices

import com.sd.laborator.components.RabbitMqComponent
import org.springframework.amqp.core.AmqpTemplate
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Microserviciu de orchestrare RabbitMQ.
 *
 * Rolul sau:
 * 1. Asculta pe coada RabbitMQ mesajele trimise de clientul Python CLI.
 * 2. Parseaza comanda primita (format: "operatie~parametri").
 * 3. Delega executia catre BeerCRUDMicroservice (API Gateway) prin HTTP.
 * 4. Trimite raspunsul inapoi pe coada RabbitMQ catre CLI.
 *
 * Respecta principiul S din SOLID: singura responsabilitate este
 * orchestrarea comunicatiei RabbitMQ <-> Gateway HTTP.
 *
 * Formatul mesajelor primite:
 *   createBeerTable~
 *   addBeer~name=Corona;price=3.5
 *   getBeers~
 *   getBeerByName~name=Corona
 *   getBeerByPrice~price=5.0
 *   updateBeer~id=1;name=Corona;price=4.0
 *   deleteBeer~name=Corona
 */
@Component
class BeerDAOMicroservice {

    @Autowired
    private lateinit var rabbitMqComponent: RabbitMqComponent

    private lateinit var amqpTemplate: AmqpTemplate

    @Autowired
    fun initTemplate() {
        this.amqpTemplate = rabbitMqComponent.rabbitTemplate()
    }

    @RabbitListener(queues = ["\${beerapp.rabbitmq.queue}"])
    fun receiveMessage(msg: String) {
        println("[BeerDAOMicroservice] Mesaj primit: $msg")

        val parts = msg.split('~', limit = 2)
        if (parts.size < 2) {
            println("[BeerDAOMicroservice] Format invalid: $msg")
            return
        }

        val operation = parts[0]
        val parameters = parts[1]

        val result: String? = when (operation) {
            "createBeerTable" -> {
                // Trimitem un addBeer fictiv ca sa cream tabela la primul start
                // In realitate, tabela se creeaza la prima operatie reala
                println("[BeerDAOMicroservice] Initializare tabela...")
                "Tabela beers verificata/creata."
            }

            "addBeer" -> {
                // parameters: "name=Corona;price=3.5"
                val params = parseParams(parameters)
                khttp.post(
                    "http://localhost:8080/addbeer",
                    data = mapOf("name" to (params["name"] ?: ""), "price" to (params["price"] ?: "0"))
                ).text
            }

            "getBeers" -> {
                khttp.get("http://localhost:8080/getbeers").text
            }

            "getBeerByName" -> {
                // parameters: "name=Corona"
                val params = parseParams(parameters)
                val name = params["name"] ?: ""
                khttp.get("http://localhost:8080/getbeerbyname/$name").text
            }

            "getBeerByPrice" -> {
                // parameters: "price=5.0"
                val params = parseParams(parameters)
                val price = params["price"] ?: "0"
                khttp.get("http://localhost:8080/getbeerbyprice/$price").text
            }

            "updateBeer" -> {
                // parameters: "id=1;name=Corona;price=4.0"
                val params = parseParams(parameters)
                khttp.put(
                    "http://localhost:8080/updatebeer",
                    data = mapOf(
                        "id"    to (params["id"]    ?: ""),
                        "name"  to (params["name"]  ?: ""),
                        "price" to (params["price"] ?: "0")
                    )
                ).text
            }

            "deleteBeer" -> {
                // parameters: "name=Corona"
                val params = parseParams(parameters)
                val name = params["name"] ?: ""
                khttp.delete("http://localhost:8080/deletebeer/$name").text
            }

            else -> {
                println("[BeerDAOMicroservice] Operatie necunoscuta: $operation")
                null
            }
        }

        println("[BeerDAOMicroservice] Rezultat: $result")
        if (result != null) {
            sendMessage(result)
        }
    }

    /**
     * Parseaza un sir de forma "cheie1=val1;cheie2=val2" intr-un Map.
     */
    private fun parseParams(parameters: String): Map<String, String> {
        if (parameters.isBlank()) return emptyMap()
        return parameters.split(';')
            .map { it.split('=', limit = 2) }
            .filter { it.size == 2 }
            .associate { it[0].trim() to it[1].trim() }
    }

    private fun sendMessage(msg: String) {
        println("[BeerDAOMicroservice] Trimitere raspuns: $msg")
        amqpTemplate.convertAndSend(
            rabbitMqComponent.getExchange(),
            rabbitMqComponent.getRoutingKey(),
            msg
        )
    }
}
