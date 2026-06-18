package com.sd.laborator.controllers

import com.sd.laborator.interfaces.IReadService
import com.sd.laborator.models.Beer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller REST pentru microserviciul READ (port 8082).
 * Respecta SRP si DIP.
 */
@RestController
@RequestMapping("/beer")
class ReadController {

    @Autowired
    private lateinit var readService: IReadService

    /** GET /beer — toate berile */
    @GetMapping
    fun getAll(): ResponseEntity<List<Beer>> =
        ResponseEntity(readService.getAll(), HttpStatus.OK)

    /** GET /beer/{id} — bere dupa id */
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Int): ResponseEntity<Any> {
        val beer = readService.getById(id)
        return if (beer != null) ResponseEntity(beer, HttpStatus.OK)
        else ResponseEntity(mapOf("error" to "Berea cu id=$id nu a fost gasita."), HttpStatus.NOT_FOUND)
    }

    /** GET /beer/name/{name} — bere dupa nume */
    @GetMapping("/name/{name}")
    fun getByName(@PathVariable name: String): ResponseEntity<Any> {
        val beer = readService.getByName(name)
        return if (beer != null) ResponseEntity(beer, HttpStatus.OK)
        else ResponseEntity(mapOf("error" to "Berea '$name' nu a fost gasita."), HttpStatus.NOT_FOUND)
    }

    /** GET /beer/price/{maxPrice} — beri cu pretul <= maxPrice */
    @GetMapping("/price/{maxPrice}")
    fun getByMaxPrice(@PathVariable maxPrice: Float): ResponseEntity<List<Beer>> =
        ResponseEntity(readService.getByMaxPrice(maxPrice), HttpStatus.OK)
}
