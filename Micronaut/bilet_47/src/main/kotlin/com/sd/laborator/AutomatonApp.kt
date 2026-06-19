package com.sd.laborator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.kafka.annotation.EnableKafka

@EnableKafka
@SpringBootApplication
class AutomatonApp

fun main(args: Array<String>) {
    runApplication<AutomatonApp>(*args)
}
