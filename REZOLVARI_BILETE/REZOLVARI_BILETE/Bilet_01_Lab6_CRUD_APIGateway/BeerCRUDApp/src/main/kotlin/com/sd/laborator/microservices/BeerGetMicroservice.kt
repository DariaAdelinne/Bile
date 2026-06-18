package com.sd.laborator.microservices

import com.sd.laborator.interfaces.BeerDAO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody

/**
 * Microserviciu responsabil EXCLUSIV pentru operatiile READ (Get).
 * Respecta principiul S din SOLID: o singura responsabilitate.
 * Endpoints:
 *   GET /getbeersmicroservice
 *   GET /getbeerbynamemicroservice/{name}
 *   GET /getbeerbypricemicroservice/{price}
 */
@Controller
class BeerGetMicroservice {

    @Autowired
    private lateinit var beerDAO: BeerDAO

    @RequestMapping(value = ["/getbeersmicroservice"], method = [RequestMethod.GET])
    @ResponseBody
    fun getBeers(): String {
        println("[BeerGetMicroservice] Returnare toate berile")
        return beerDAO.getBeers()
    }

    @RequestMapping(value = ["/getbeerbynamemicroservice/{name}"], method = [RequestMethod.GET])
    @ResponseBody
    fun getBeerByName(@PathVariable name: String): String {
        println("[BeerGetMicroservice] Cautare bere dupa nume: $name")
        return beerDAO.getBeerByName(name) ?: "Bere negasita."
    }

    @RequestMapping(value = ["/getbeerbypricemicroservice/{price}"], method = [RequestMethod.GET])
    @ResponseBody
    fun getBeerByPrice(@PathVariable price: Float): String {
        println("[BeerGetMicroservice] Cautare beri cu pret <= $price")
        return beerDAO.getBeerByPrice(price)
    }
}
