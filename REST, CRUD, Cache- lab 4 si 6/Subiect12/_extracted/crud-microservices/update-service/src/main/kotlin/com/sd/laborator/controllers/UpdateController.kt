package com.sd.laborator.controllers

import com.sd.laborator.interfaces.IUpdateService
import com.sd.laborator.models.Beer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller REST pentru microserviciul UPDATE (port 8083).
 * Respecta SRP si DIP.
 */
@RestController
@RequestMapping("/beer")
class UpdateController {

    @Autowired
    private lateinit var updateService: IUpdateService

    /** PUT /beer/{id} — actualizeaza berea cu id-ul dat */
    @PutMapping("/{id}")
    fun updateBeer(@PathVariable id: Int, @RequestBody beer: Beer): ResponseEntity<Any> {
        return try {
            val updated = updateService.updateBeer(id, beer)
            if (updated) ResponseEntity(mapOf("message" to "Berea actualizata."), HttpStatus.OK)
            else ResponseEntity(mapOf("error" to "Berea cu id=$id nu exista."), HttpStatus.NOT_FOUND)
        } catch (e: IllegalArgumentException) {
            ResponseEntity(mapOf("error" to e.message), HttpStatus.BAD_REQUEST)
        }
    }
}
