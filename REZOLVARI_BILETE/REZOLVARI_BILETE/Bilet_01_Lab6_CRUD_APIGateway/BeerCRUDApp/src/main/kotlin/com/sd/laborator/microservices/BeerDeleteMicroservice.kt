package com.sd.laborator.microservices

import com.sd.laborator.interfaces.BeerDAO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody

/**
 * Microserviciu responsabil EXCLUSIV pentru operatia DELETE.
 * Respecta principiul S din SOLID: o singura responsabilitate.
 * Endpoint: DELETE /deletebeermicroservice/{name}
 */
@Controller
class BeerDeleteMicroservice {

    @Autowired
    private lateinit var beerDAO: BeerDAO

    @RequestMapping(value = ["/deletebeermicroservice/{name}"], method = [RequestMethod.DELETE])
    @ResponseBody
    fun deleteBeer(@PathVariable name: String): String {
        beerDAO.deleteBeer(name)
        println("[BeerDeleteMicroservice] Bere stearsa: $name")
        return "Beer '$name' stearsa cu succes."
    }
}
