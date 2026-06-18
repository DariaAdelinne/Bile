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
 * Microserviciu responsabil EXCLUSIV pentru operatia UPDATE.
 * Respecta principiul S din SOLID: o singura responsabilitate.
 * Endpoint: PUT /updatebeermicroservice
 */
@Controller
class BeerUpdateMicroservice {

    @Autowired
    private lateinit var beerDAO: BeerDAO

    @RequestMapping(value = ["/updatebeermicroservice"], method = [RequestMethod.PUT])
    @ResponseBody
    fun updateBeer(@RequestParam beer: Map<String, String>): String {
        beerDAO.updateBeer(
            Beer(
                id = beer["id"]?.toInt() ?: -1,
                name = beer["name"].toString(),
                price = beer["price"]?.toFloat() ?: 0f
            )
        )
        println("[BeerUpdateMicroservice] Bere actualizata: id=${beer["id"]}, name=${beer["name"]}, price=${beer["price"]}")
        return "Beer cu id=${beer["id"]} actualizata cu succes."
    }
}
