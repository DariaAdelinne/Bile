package com.sd.laborator

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HelloController(
    private val helloService: HelloService //Constructor, HelloController vede ca are nevoie de HelloService si il injecteaza
) {
    @GetMapping("/helloworld")
    fun hello(): String = helloService.getHello()
}