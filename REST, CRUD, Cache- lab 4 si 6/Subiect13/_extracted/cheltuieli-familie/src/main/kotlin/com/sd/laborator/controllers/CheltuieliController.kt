package com.sd.laborator.controllers

import com.sd.laborator.gateway.ApiGatewayService
import com.sd.laborator.pojo.Cheltuiala
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller REST pentru gestiunea cheltuielilor de familie.
 *
 * Respecta SRP: se ocupa DOAR de maparea HTTP -> business, nu de logica.
 * Respecta DIP: depinde de ApiGatewayService (abstractizare), nu de
 *               implementarile concrete ale serviciilor.
 *
 * Toate cererile trec prin ApiGatewayService care gestioneaza
 * cache-ul si replicarea in mod transparent.
 *
 * Endpoint-uri:
 *   POST   /cheltuiala              - creeaza cheltuiala
 *   GET    /cheltuiala/{id}         - returneaza cheltuiala (cu cache)
 *   PUT    /cheltuiala/{id}         - actualizeaza cheltuiala
 *   DELETE /cheltuiala/{id}         - sterge cheltuiala
 *   GET    /cheltuieli              - lista cu filtre optionale (cu cache)
 *   GET    /cheltuieli/total        - total pe categorie (cu cache)
 *   GET    /cache/info              - diagnosticare cache
 *   POST   /cache/sync              - sincronizare manuala replici
 */
@RestController
class CheltuieliController {

    @Autowired
    private lateinit var gateway: ApiGatewayService

    // ── CRUD ─────────────────────────────────────────────────────────────

    @PostMapping("/cheltuiala")
    fun createCheltuiala(@RequestBody cheltuiala: Cheltuiala): ResponseEntity<Unit> {
        gateway.createCheltuiala(cheltuiala)
        return ResponseEntity(Unit, HttpStatus.CREATED)
    }

    @GetMapping("/cheltuiala/{id}")
    fun getCheltuiala(@PathVariable id: Int): ResponseEntity<Map<String, Any?>> {
        val (cheltuiala, dinCache) = gateway.getCheltuiala(id)
        return if (cheltuiala == null) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        } else {
            ResponseEntity(
                mapOf("cheltuiala" to cheltuiala, "dinCache" to dinCache),
                HttpStatus.OK
            )
        }
    }

    @PutMapping("/cheltuiala/{id}")
    fun updateCheltuiala(
        @PathVariable id: Int,
        @RequestBody cheltuiala: Cheltuiala
    ): ResponseEntity<Unit> {
        val (existing, _) = gateway.getCheltuiala(id)
        return if (existing == null) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        } else {
            gateway.updateCheltuiala(id, cheltuiala)
            ResponseEntity(Unit, HttpStatus.ACCEPTED)
        }
    }

    @DeleteMapping("/cheltuiala/{id}")
    fun deleteCheltuiala(@PathVariable id: Int): ResponseEntity<Unit> {
        val (existing, _) = gateway.getCheltuiala(id)
        return if (existing == null) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        } else {
            gateway.deleteCheltuiala(id)
            ResponseEntity(Unit, HttpStatus.OK)
        }
    }

    // ── QUERY ────────────────────────────────────────────────────────────

    @GetMapping("/cheltuieli")
    fun getCheltuieli(
        @RequestParam(required = false, defaultValue = "") categorie: String,
        @RequestParam(required = false, defaultValue = "") platitDe: String
    ): ResponseEntity<Map<String, Any>> {
        val (lista, dinCache) = gateway.getCheltuieli(categorie, platitDe)
        val status = if (lista.isEmpty()) HttpStatus.NO_CONTENT else HttpStatus.OK
        return ResponseEntity(
            mapOf("cheltuieli" to lista, "dinCache" to dinCache),
            status
        )
    }

    @GetMapping("/cheltuieli/total")
    fun getTotalCategorie(
        @RequestParam(required = true) categorie: String
    ): ResponseEntity<Map<String, Any>> {
        val (total, dinCache) = gateway.getTotalCategorie(categorie)
        return ResponseEntity(
            mapOf("categorie" to categorie, "total" to total, "dinCache" to dinCache),
            HttpStatus.OK
        )
    }

    // ── CACHE / REPLICARE ────────────────────────────────────────────────

    @GetMapping("/cache/info")
    fun getCacheInfo(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity(gateway.getCacheInfo(), HttpStatus.OK)
    }

    @PostMapping("/cache/sync")
    fun syncCache(): ResponseEntity<Map<String, String>> {
        gateway.syncCache()
        return ResponseEntity(
            mapOf("status" to "Cache sincronizat cu succes intre toate replicile"),
            HttpStatus.OK
        )
    }
}
