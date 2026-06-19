package com.sd.laborator

import com.sd.laborator.functions.XkcdRssConsumer
import com.sd.laborator.functions.XkcdRssProducer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import kotlin.system.exitProcess

/**
 * Aplicatie producator-consumator serverless.
 *
 * Flux de executie:
 *   1. XkcdRssProducer (Supplier<String>) — preia fluxul Atom de la xkcd.com
 *   2. XkcdRssConsumer (Consumer<String>) — parseaza XML si afiseaza perechile <TITLE, URL>
 *
 * Ambele functii sunt bean-uri Spring (@Component) si sunt injectate
 * prin contextul aplicatiei — respecta principiul DIP.
 */
@SpringBootApplication
class Application

fun main(args: Array<String>) {
    val ctx = runApplication<Application>(*args)

    // Obtine bean-urile din contextul Spring (injectie de dependenta)
    val producer = ctx.getBean(XkcdRssProducer::class.java)
    val consumer = ctx.getBean(XkcdRssConsumer::class.java)

    // Flux producator -> consumator
    val xml = producer.get()   // Supplier<String>.get()
    consumer.accept(xml)       // Consumer<String>.accept()

    exitProcess(0)
}
