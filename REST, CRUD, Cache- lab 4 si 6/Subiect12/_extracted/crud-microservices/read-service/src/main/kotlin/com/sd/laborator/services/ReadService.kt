package com.sd.laborator.services

import com.sd.laborator.interfaces.IReadService
import com.sd.laborator.models.Beer
import com.sd.laborator.repository.IBeerRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Microserviciul READ.
 * Respecta SRP: se ocupa DOAR de citirea datelor.
 * Respecta DIP: depinde de IBeerRepository.
 */
@Service
class ReadService : IReadService {

    @Autowired
    private lateinit var repository: IBeerRepository

    override fun getAll(): List<Beer> = repository.getAll()

    override fun getById(id: Int): Beer? = repository.getById(id)

    override fun getByName(name: String): Beer? = repository.getByName(name)

    override fun getByMaxPrice(maxPrice: Float): List<Beer> =
        repository.getAll().filter { it.price <= maxPrice }
}
