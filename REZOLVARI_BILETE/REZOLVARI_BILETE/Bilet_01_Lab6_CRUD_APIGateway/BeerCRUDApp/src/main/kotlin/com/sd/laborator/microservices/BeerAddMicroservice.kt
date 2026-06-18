package com.sd.laborator.microservices

import com.sd.laborator.interfaces.BeerDAO
import com.sd.laborator.model.Beer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody

/**
 * Microserviciu responsabil EXCLUSIV pentru operatia CREATE (Add).
 * Respecta principiul S din SOLID: o singura responsabilitate.
 * Endpoint: POST /addbeermicroservice
 */
@Controller
class BeerAddMicroservice {

    @Autowired
    private lateinit var beerDAO: BeerDAO

    @RequestMapping(value = ["/addbeermicroservice"], method = [RequestMethod.POST])
    @ResponseBody
    fun addBeer(@RequestParam beer: Map<String, String>): String {
        beerDAO.addBeer(
            Beer(
                id = -1,
                name = beer["name"].toString(),
                price = beer["price"]?.toFloat() ?: 0f
            )
        )
        println("[BeerAddMicroservice] Bere adaugata: ${beer["name"]}, pret: ${beer["price"]}")
        return "Beer '${beer["name"]}' adaugata cu succes."
    }
}
