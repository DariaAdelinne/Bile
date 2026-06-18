package com.sd.laborator

import org.springframework.stereotype.Service

@Service // SPring creeaza obiect din calasa HelloService si il injecteaza in alte clase
class HelloService {
    fun getHello() = "Hello World!"
}