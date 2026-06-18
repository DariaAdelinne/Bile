package com.sd.laborator.microservices

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.fge.jsonpatch.JsonPatch
import com.sd.laborator.interfaces.IAgendaService
import com.sd.laborator.interfaces.ICacheService
import com.sd.laborator.pojo.Person
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

// Microserviciul poarta API cu cache:
// intercepteaza cererile, returneaza din cache daca cererea a mai aparut in ultimele 30 de minute,
// altfel delega catre AgendaService si stocheaza rezultatul in cache
@RestController
@RequestMapping("/cached")
class CacheGatewayMicroservice {

    @Autowired
    private lateinit var agendaService: IAgendaService

    @Autowired
    private lateinit var cacheService: ICacheService

    @GetMapping("/person/{id}")
    fun getPerson(@PathVariable id: Int): ResponseEntity<Any?> {
        val key = "getPerson_$id"
        val cached = cacheService.get(key)
        if (cached != null && cacheService.isValid(cached)) {
            println("[CACHE HIT] $key")
            return ResponseEntity(cached.responseBody, HttpStatus.OK)
        }

        val person = agendaService.getPerson(id)
        cacheService.put(key, person)
        println("[CACHE MISS] $key -> stocat in cache")
        return if (person != null)
            ResponseEntity(person, HttpStatus.OK)
        else
            ResponseEntity(null, HttpStatus.NOT_FOUND)
    }

    @PostMapping("/person")
    fun createPerson(@RequestBody person: Person): ResponseEntity<String> {
        val key = "createPerson_${person.id}"
        val cached = cacheService.get(key)
        if (cached != null && cacheService.isValid(cached)) {
            println("[CACHE HIT] $key - cerere duplicata in ultimele 30 min, ignorata")
            return ResponseEntity("Cerere duplicata in ultimele 30 de minute. Nu s-a adaugat din nou.", HttpStatus.OK)
        }

        agendaService.createPerson(person)
        cacheService.put(key, "created")
        println("[CACHE MISS] $key -> persoana adaugata si stocat in cache")
        return ResponseEntity("Persoana adaugata cu succes.", HttpStatus.CREATED)
    }

    @PutMapping("/person/{id}")
    fun updatePerson(@PathVariable id: Int, @RequestBody person: Person): ResponseEntity<String> {
        val key = "updatePerson_${id}_${person.hashCode()}"
        val cached = cacheService.get(key)
        if (cached != null && cacheService.isValid(cached)) {
            println("[CACHE HIT] $key - cerere duplicata in ultimele 30 min, ignorata")
            return ResponseEntity("Cerere duplicata in ultimele 30 de minute.", HttpStatus.OK)
        }

        agendaService.getPerson(id) ?: return ResponseEntity("Persoana cu id=$id nu exista.", HttpStatus.NOT_FOUND)
        agendaService.updatePerson(id, person)
        cacheService.put(key, "updated")
        println("[CACHE MISS] $key -> persoana actualizata si stocat in cache")
        return ResponseEntity("Persoana actualizata cu succes.", HttpStatus.ACCEPTED)
    }

    @PatchMapping("/person/{id}")
    fun patchPerson(@PathVariable id: Int, @RequestBody patchOperations: JsonPatch): ResponseEntity<String> {
        val key = "patchPerson_${id}_${patchOperations.hashCode()}"
        val cached = cacheService.get(key)
        if (cached != null && cacheService.isValid(cached)) {
            println("[CACHE HIT] $key - cerere duplicata in ultimele 30 min, ignorata")
            return ResponseEntity("Cerere duplicata in ultimele 30 de minute.", HttpStatus.OK)
        }

        val existing = agendaService.getPerson(id) ?: return ResponseEntity("Persoana cu id=$id nu exista.", HttpStatus.NOT_FOUND)
        val objectMapper = ObjectMapper()
        val patched = patchOperations.apply(objectMapper.valueToTree(existing))
        val patchedPerson = objectMapper.treeToValue(patched, Person::class.java)
        agendaService.updatePerson(id, patchedPerson)
        cacheService.put(key, "patched")
        println("[CACHE MISS] $key -> persoana patch-uita si stocat in cache")
        return ResponseEntity("Persoana modificata cu succes.", HttpStatus.NO_CONTENT)
    }

    @DeleteMapping("/person/{id}")
    fun deletePerson(@PathVariable id: Int): ResponseEntity<String> {
        val key = "deletePerson_$id"
        val cached = cacheService.get(key)
        if (cached != null && cacheService.isValid(cached)) {
            println("[CACHE HIT] $key - cerere duplicata in ultimele 30 min, ignorata")
            return ResponseEntity("Cerere duplicata in ultimele 30 de minute.", HttpStatus.OK)
        }

        agendaService.getPerson(id) ?: return ResponseEntity("Persoana cu id=$id nu exista.", HttpStatus.NOT_FOUND)
        agendaService.deletePerson(id)
        cacheService.put(key, "deleted")
        println("[CACHE MISS] $key -> persoana stearsa si stocat in cache")
        return ResponseEntity("Persoana stearsa cu succes.", HttpStatus.OK)
    }

    @GetMapping("/search")
    fun searchAgenda(
        @RequestParam(required = false, defaultValue = "") lastName: String,
        @RequestParam(required = false, defaultValue = "") firstName: String,
        @RequestParam(required = false, defaultValue = "") telephone: String
    ): ResponseEntity<Any> {
        val key = "searchAgenda_${lastName}_${firstName}_${telephone}"
        val cached = cacheService.get(key)
        if (cached != null && cacheService.isValid(cached)) {
            println("[CACHE HIT] $key")
            return ResponseEntity(cached.responseBody, HttpStatus.OK)
        }

        val results = agendaService.searchAgenda(lastName, firstName, telephone)
        cacheService.put(key, results)
        println("[CACHE MISS] $key -> rezultate stocate in cache")
        return if (results.isEmpty())
            ResponseEntity(results, HttpStatus.NO_CONTENT)
        else
            ResponseEntity(results, HttpStatus.OK)
    }
}
