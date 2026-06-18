package com.sd.laborator.services

import com.sd.laborator.interfaces.ICreateService
import com.sd.laborator.models.Beer
import com.sd.laborator.repository.IBeerRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.regex.Pattern

/**
 * Microserviciul CREATE.
 * Respecta SRP: se ocupa DOAR de adaugarea berii.
 * Respecta OCP: extensibil (ex: validari extra) fara a modifica interfata.
 * Respecta DIP: depinde de IBeerRepository (abstractizare).
 */
@Service
class CreateService : ICreateService {

    @Autowired
    private lateinit var repository: IBeerRepository

    private val safePattern = Pattern.compile("\\W")

    override fun initTable() {
        repository.initTable()
    }

    override fun addBeer(beer: Beer): Beer {
        if (safePattern.matcher(beer.name).find()) {
            throw IllegalArgumentException("Nume invalid (potential SQL injection): ${beer.name}")
        }
        repository.add(beer)
        return repository.getByName(beer.name) ?: beer
    }
}
