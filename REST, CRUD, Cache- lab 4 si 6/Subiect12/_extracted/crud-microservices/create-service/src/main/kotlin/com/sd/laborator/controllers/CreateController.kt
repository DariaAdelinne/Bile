package com.sd.laborator.controllers

import com.sd.laborator.interfaces.ICreateService
import com.sd.laborator.models.Beer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller REST pentru microserviciul CREATE (port 8081).
 * Respecta SRP: mapare HTTP -> logica de create.
 * Respecta DIP: depinde de ICreateService.
 */
@RestController
@RequestMapping("/beer")
class CreateController {

    @Autowired
    private lateinit var createService: ICreateService

    /** Initializeaza tabela (apelat la pornire de gateway) */
    @PostMapping("/init")
    fun initTable(): ResponseEntity<String> {
        createService.initTable()
        return ResponseEntity("Tabela beers initializata.", HttpStatus.OK)
    }

    /** POST /beer — adauga o bere noua */
    @PostMapping
    fun addBeer(@RequestBody beer: Beer): ResponseEntity<Any> {
        return try {
            val saved = createService.addBeer(beer)
            ResponseEntity(saved, HttpStatus.CREATED)
        } catch (e: IllegalArgumentException) {
            ResponseEntity(mapOf("error" to e.message), HttpStatus.BAD_REQUEST)
        }
    }
}
