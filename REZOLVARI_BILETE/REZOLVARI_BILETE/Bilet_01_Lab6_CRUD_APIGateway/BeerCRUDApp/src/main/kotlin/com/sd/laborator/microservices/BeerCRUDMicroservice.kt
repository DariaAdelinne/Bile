package com.sd.laborator.microservices

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody

/**
 * POARTA API (API Gateway) - conform cerintei biletului.
 *
 * Acest microserviciu actioneaza ca punct central de intrare pentru toate
 * operatiile CRUD. El NU contine logica de business, ci doar delega
 * cererile catre microserviciile specializate (Add, Get, Update, Delete).
 *
 * Respecta principiul S din SOLID: singura responsabilitate este rutarea.
 * Respecta principiul O din SOLID: deschis pentru extindere (noi microservicii
 * pot fi adaugate fara a modifica gateway-ul).
 *
 * Endpoints expuse:
 *   GET    /getbeers
 *   GET    /getbeerbyname/{name}
 *   GET    /getbeerbyprice/{price}
 *   POST   /addbeer
 *   PUT    /updatebeer
 *   DELETE /deletebeer/{name}
 */
@Controller
class BeerCRUDMicroservice {

    // --- READ ---

    @RequestMapping(value = ["/getbeers"], method = [RequestMethod.GET])
    @ResponseBody
    fun getBeers(): String {
        println("[BeerCRUDMicroservice/Gateway] -> GET /getbeersmicroservice")
        return khttp.get("http://localhost:8080/getbeersmicroservice").text
    }

    @RequestMapping(value = ["/getbeerbyname/{name}"], method = [RequestMethod.GET])
    @ResponseBody
    fun getBeerByName(@PathVariable name: String): String {
        println("[BeerCRUDMicroservice/Gateway] -> GET /getbeerbynamemicroservice/$name")
        return khttp.get("http://localhost:8080/getbeerbynamemicroservice/$name").text
    }

    @RequestMapping(value = ["/getbeerbyprice/{price}"], method = [RequestMethod.GET])
    @ResponseBody
    fun getBeerByPrice(@PathVariable price: Float): String {
        println("[BeerCRUDMicroservice/Gateway] -> GET /getbeerbypricemicroservice/$price")
        return khttp.get("http://localhost:8080/getbeerbypricemicroservice/$price").text
    }

    // --- CREATE ---

    @RequestMapping(value = ["/addbeer"], method = [RequestMethod.POST])
    @ResponseBody
    fun addBeer(@RequestParam beer: Map<String, String>): String {
        println("[BeerCRUDMicroservice/Gateway] -> POST /addbeermicroservice $beer")
        return khttp.post("http://localhost:8080/addbeermicroservice", data = beer).text
    }

    // --- UPDATE ---

    @RequestMapping(value = ["/updatebeer"], method = [RequestMethod.PUT])
    @ResponseBody
    fun updateBeer(@RequestParam beer: Map<String, String>): String {
        println("[BeerCRUDMicroservice/Gateway] -> PUT /updatebeermicroservice $beer")
        return khttp.put("http://localhost:8080/updatebeermicroservice", data = beer).text
    }

    // --- DELETE ---

    @RequestMapping(value = ["/deletebeer/{name}"], method = [RequestMethod.DELETE])
    @ResponseBody
    fun deleteBeer(@PathVariable name: String): String {
        println("[BeerCRUDMicroservice/Gateway] -> DELETE /deletebeermicroservice/$name")
        return khttp.delete("http://localhost:8080/deletebeermicroservice/$name").text
    }
}
