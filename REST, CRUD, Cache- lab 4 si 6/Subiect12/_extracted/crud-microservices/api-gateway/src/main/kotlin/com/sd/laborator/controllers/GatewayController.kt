package com.sd.laborator.controllers

import com.sd.laborator.interfaces.IGatewayService
import com.sd.laborator.models.Beer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller REST al API Gateway-ului (port 8080).
 *
 * Este SINGURUL punct de intrare vizibil clientilor.
 * Toate cererile sunt rutate catre microserviciile CRUD corespunzatoare
 * prin intermediul GatewayService.
 *
 * Respecta SRP: mapare HTTP -> gateway, fara logica de business.
 * Respecta DIP: depinde de IGatewayService (interfata), nu de implementare.
 *
 * Endpoint-uri expuse:
 *   POST   /api/beer/init           — initializare tabela
 *   POST   /api/beer                — adauga bere (-> CreateService :8081)
 *   GET    /api/beer                — toate berile (-> ReadService   :8082)
 *   GET    /api/beer/{id}           — bere dupa id
 *   GET    /api/beer/name/{name}    — bere dupa nume
 *   GET    /api/beer/price/{price}  — beri cu pret <= price
 *   PUT    /api/beer/{id}           — actualizeaza (-> UpdateService :8083)
 *   DELETE /api/beer/{id}           — sterge      (-> DeleteService  :8084)
 */
@RestController
@RequestMapping("/api/beer")
class GatewayController {

    @Autowired
    private lateinit var gatewayService: IGatewayService

    @PostMapping("/init")
    fun initTable(): ResponseEntity<Any> =
        ResponseEntity(gatewayService.initTable(), HttpStatus.OK)

    @PostMapping
    fun addBeer(@RequestBody beer: Beer): ResponseEntity<Any> =
        ResponseEntity(gatewayService.addBeer(beer), HttpStatus.CREATED)

    @GetMapping
    fun getAllBeers(): ResponseEntity<Any> =
        ResponseEntity(gatewayService.getAllBeers(), HttpStatus.OK)

    @GetMapping("/{id}")
    fun getBeerById(@PathVariable id: Int): ResponseEntity<Any> =
        ResponseEntity(gatewayService.getBeerById(id), HttpStatus.OK)

    @GetMapping("/name/{name}")
    fun getBeerByName(@PathVariable name: String): ResponseEntity<Any> =
        ResponseEntity(gatewayService.getBeerByName(name), HttpStatus.OK)

    @GetMapping("/price/{maxPrice}")
    fun getBeersByPrice(@PathVariable maxPrice: Float): ResponseEntity<Any> =
        ResponseEntity(gatewayService.getBeersByMaxPrice(maxPrice), HttpStatus.OK)

    @PutMapping("/{id}")
    fun updateBeer(@PathVariable id: Int, @RequestBody beer: Beer): ResponseEntity<Any> =
        ResponseEntity(gatewayService.updateBeer(id, beer), HttpStatus.OK)

    @DeleteMapping("/{id}")
    fun deleteBeer(@PathVariable id: Int): ResponseEntity<Any> =
        ResponseEntity(gatewayService.deleteBeer(id), HttpStatus.OK)
}
