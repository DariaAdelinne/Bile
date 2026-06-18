package com.sd.laborator.controllers

import com.sd.laborator.interfaces.IDeleteService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller REST pentru microserviciul DELETE (port 8084).
 * Respecta SRP si DIP.
 */
@RestController
@RequestMapping("/beer")
class DeleteController {

    @Autowired
    private lateinit var deleteService: IDeleteService

    /** DELETE /beer/{id} — sterge berea cu id-ul dat */
    @DeleteMapping("/{id}")
    fun deleteBeer(@PathVariable id: Int): ResponseEntity<Any> {
        val deleted = deleteService.deleteBeer(id)
        return if (deleted)
            ResponseEntity(mapOf("message" to "Berea cu id=$id a fost stearsa."), HttpStatus.OK)
        else
            ResponseEntity(mapOf("error" to "Berea cu id=$id nu exista."), HttpStatus.NOT_FOUND)
    }
}
