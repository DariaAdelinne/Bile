package com.sd.laborator.services

import com.sd.laborator.interfaces.IGatewayService
import com.sd.laborator.models.Beer
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

/**
 * Implementarea API Gateway-ului.
 *
 * Orchestreaza apelurile HTTP catre cele 4 microservicii CRUD:
 *   - CreateService  (port 8081) — POST
 *   - ReadService    (port 8082) — GET
 *   - UpdateService  (port 8083) — PUT
 *   - DeleteService  (port 8084) — DELETE
 *
 * Respecta SRP: se ocupa DOAR de rutare si orchestrare.
 * Respecta OCP: adaugarea unui nou microserviciu nu modifica interfata.
 * Respecta DIP: controller-ul depinde de IGatewayService, nu de aceasta clasa.
 *
 * Foloseste RestTemplate pentru comunicarea HTTP inter-servicii.
 */
@Service
class GatewayService : IGatewayService {

    @Value("\${crud.create.url}")
    private lateinit var createUrl: String

    @Value("\${crud.read.url}")
    private lateinit var readUrl: String

    @Value("\${crud.update.url}")
    private lateinit var updateUrl: String

    @Value("\${crud.delete.url}")
    private lateinit var deleteUrl: String

    private val restTemplate = RestTemplate()

    private fun jsonHeaders() = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
    }

    override fun initTable(): Any {
        return try {
            val response = restTemplate.postForEntity(
                "$createUrl/beer/init", null, String::class.java
            )
            mapOf("status" to "ok", "message" to response.body)
        } catch (e: Exception) {
            mapOf("error" to "CreateService indisponibil: ${e.message}")
        }
    }

    // ── CREATE ────────────────────────────────────────────────────────────

    override fun addBeer(beer: Beer): Any {
        return try {
            val entity = HttpEntity(beer, jsonHeaders())
            val response = restTemplate.postForEntity(
                "$createUrl/beer", entity, Any::class.java
            )
            response.body ?: mapOf("error" to "Raspuns gol de la CreateService")
        } catch (e: HttpClientErrorException) {
            mapOf("error" to e.responseBodyAsString)
        } catch (e: Exception) {
            mapOf("error" to "CreateService indisponibil: ${e.message}")
        }
    }

    // ── READ ──────────────────────────────────────────────────────────────

    override fun getAllBeers(): Any {
        return try {
            val response = restTemplate.getForEntity(
                "$readUrl/beer", Any::class.java
            )
            response.body ?: emptyList<Any>()
        } catch (e: Exception) {
            mapOf("error" to "ReadService indisponibil: ${e.message}")
        }
    }

    override fun getBeerById(id: Int): Any {
        return try {
            val response = restTemplate.getForEntity(
                "$readUrl/beer/$id", Any::class.java
            )
            response.body ?: mapOf("error" to "Nu s-a gasit berea cu id=$id")
        } catch (e: HttpClientErrorException.NotFound) {
            mapOf("error" to "Berea cu id=$id nu a fost gasita.")
        } catch (e: Exception) {
            mapOf("error" to "ReadService indisponibil: ${e.message}")
        }
    }

    override fun getBeerByName(name: String): Any {
        return try {
            val response = restTemplate.getForEntity(
                "$readUrl/beer/name/$name", Any::class.java
            )
            response.body ?: mapOf("error" to "Nu s-a gasit berea '$name'")
        } catch (e: HttpClientErrorException.NotFound) {
            mapOf("error" to "Berea '$name' nu a fost gasita.")
        } catch (e: Exception) {
            mapOf("error" to "ReadService indisponibil: ${e.message}")
        }
    }

    override fun getBeersByMaxPrice(maxPrice: Float): Any {
        return try {
            val response = restTemplate.getForEntity(
                "$readUrl/beer/price/$maxPrice", Any::class.java
            )
            response.body ?: emptyList<Any>()
        } catch (e: Exception) {
            mapOf("error" to "ReadService indisponibil: ${e.message}")
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────────────

    override fun updateBeer(id: Int, beer: Beer): Any {
        return try {
            val entity = HttpEntity(beer, jsonHeaders())
            val response = restTemplate.exchange(
                "$updateUrl/beer/$id", HttpMethod.PUT, entity, Any::class.java
            )
            response.body ?: mapOf("message" to "Actualizat.")
        } catch (e: HttpClientErrorException.NotFound) {
            mapOf("error" to "Berea cu id=$id nu a fost gasita.")
        } catch (e: Exception) {
            mapOf("error" to "UpdateService indisponibil: ${e.message}")
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────

    override fun deleteBeer(id: Int): Any {
        return try {
            val response = restTemplate.exchange(
                "$deleteUrl/beer/$id", HttpMethod.DELETE, null, Any::class.java
            )
            response.body ?: mapOf("message" to "Sters.")
        } catch (e: HttpClientErrorException.NotFound) {
            mapOf("error" to "Berea cu id=$id nu a fost gasita.")
        } catch (e: Exception) {
            mapOf("error" to "DeleteService indisponibil: ${e.message}")
        }
    }
}
