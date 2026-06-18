package com.sd.laborator.microservices

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.fge.jsonpatch.JsonPatch
import com.sd.laborator.interfaces.IAgendaService
import com.sd.laborator.pojo.Person
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

// Microserviciul de agenda: acces direct CRUD la date, fara cache
@RestController
@RequestMapping("/agenda")
class AgendaMicroservice {

    @Autowired
    private lateinit var agendaService: IAgendaService

    @PostMapping("/person")
    fun createPerson(@RequestBody person: Person): ResponseEntity<Unit> {
        agendaService.createPerson(person)
        return ResponseEntity(Unit, HttpStatus.CREATED)
    }

    @GetMapping("/person/{id}")
    fun getPerson(@PathVariable id: Int): ResponseEntity<Person?> {
        val person = agendaService.getPerson(id)
        return if (person != null)
            ResponseEntity(person, HttpStatus.OK)
        else
            ResponseEntity(null, HttpStatus.NOT_FOUND)
    }

    @PutMapping("/person/{id}")
    fun updatePerson(@PathVariable id: Int, @RequestBody person: Person): ResponseEntity<Unit> {
        agendaService.getPerson(id) ?: return ResponseEntity(Unit, HttpStatus.NOT_FOUND)
        agendaService.updatePerson(id, person)
        return ResponseEntity(Unit, HttpStatus.ACCEPTED)
    }

    @PatchMapping("/person/{id}")
    fun patchPerson(@PathVariable id: Int, @RequestBody patchOperations: JsonPatch): ResponseEntity<Unit> {
        val existing = agendaService.getPerson(id) ?: return ResponseEntity(Unit, HttpStatus.NOT_FOUND)
        val objectMapper = ObjectMapper()
        val patched = patchOperations.apply(objectMapper.valueToTree(existing))
        val patchedPerson = objectMapper.treeToValue(patched, Person::class.java)
        agendaService.updatePerson(id, patchedPerson)
        return ResponseEntity(Unit, HttpStatus.NO_CONTENT)
    }

    @DeleteMapping("/person/{id}")
    fun deletePerson(@PathVariable id: Int): ResponseEntity<Unit> {
        agendaService.getPerson(id) ?: return ResponseEntity(Unit, HttpStatus.NOT_FOUND)
        agendaService.deletePerson(id)
        return ResponseEntity(Unit, HttpStatus.OK)
    }

    @GetMapping("/search")
    fun searchAgenda(
        @RequestParam(required = false, defaultValue = "") lastName: String,
        @RequestParam(required = false, defaultValue = "") firstName: String,
        @RequestParam(required = false, defaultValue = "") telephone: String
    ): ResponseEntity<List<Person>> {
        val results = agendaService.searchAgenda(lastName, firstName, telephone)
        return if (results.isEmpty())
            ResponseEntity(results, HttpStatus.NO_CONTENT)
        else
            ResponseEntity(results, HttpStatus.OK)
    }
}
