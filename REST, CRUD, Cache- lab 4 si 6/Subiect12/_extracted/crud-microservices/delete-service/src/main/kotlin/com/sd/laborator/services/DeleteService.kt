package com.sd.laborator.services

import com.sd.laborator.interfaces.IDeleteService
import com.sd.laborator.repository.IBeerRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Microserviciul DELETE.
 * Respecta SRP: se ocupa DOAR de stergerea datelor.
 * Respecta DIP: depinde de IBeerRepository.
 */
@Service
class DeleteService : IDeleteService {

    @Autowired
    private lateinit var repository: IBeerRepository

    override fun deleteBeer(id: Int): Boolean {
        val existing = repository.getById(id) ?: return false
        repository.delete(existing.id)
        return true
    }
}
