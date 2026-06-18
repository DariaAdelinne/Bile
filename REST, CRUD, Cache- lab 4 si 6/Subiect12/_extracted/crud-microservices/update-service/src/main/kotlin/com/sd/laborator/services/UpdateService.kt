package com.sd.laborator.services

import com.sd.laborator.interfaces.IUpdateService
import com.sd.laborator.models.Beer
import com.sd.laborator.repository.IBeerRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.regex.Pattern

/**
 * Microserviciul UPDATE.
 * Respecta SRP: se ocupa DOAR de actualizarea datelor.
 * Respecta DIP: depinde de IBeerRepository.
 */
@Service
class UpdateService : IUpdateService {

    @Autowired
    private lateinit var repository: IBeerRepository

    private val safePattern = Pattern.compile("\\W")

    override fun updateBeer(id: Int, beer: Beer): Boolean {
        if (safePattern.matcher(beer.name).find()) {
            throw IllegalArgumentException("Nume invalid: ${beer.name}")
        }
        val existing = repository.getById(id) ?: return false
        beer.id = existing.id
        repository.update(beer)
        return true
    }
}
