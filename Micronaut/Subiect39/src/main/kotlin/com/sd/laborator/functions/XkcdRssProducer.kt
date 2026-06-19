package com.sd.laborator.functions

import org.springframework.stereotype.Component
import java.net.URL
import java.util.function.Supplier

/**
 * Functie serverless de tip Producator (Supplier).
 *
 * Responsabilitate (SRP): preia fluxul RSS Atom de la xkcd.com si
 * returneaza continutul XML ca String catre consumator.
 *
 * Principii SOLID:
 *   S - singura responsabilitate: fetch RSS
 *   O - comportamentul de fetch poate fi extins fara a modifica consumatorul
 *   D - returneaza String (tip primitiv), consumatorul nu depinde de aceasta clasa direct
 */
@Component
class XkcdRssProducer : Supplier<String> {

    companion object {
        private const val XKCD_RSS_URL = "https://xkcd.com/atom.xml"
        private const val TIMEOUT_MS = 10_000
    }

    /**
     * Preia fluxul Atom RSS de la xkcd.com.
     * @return continutul XML ca String
     */
    override fun get(): String {
        println("[XkcdRssProducer] Preiau fluxul RSS de la $XKCD_RSS_URL ...")

        val connection = URL(XKCD_RSS_URL).openConnection()
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.setRequestProperty("User-Agent", "Mozilla/5.0")

        val xml = connection.getInputStream().bufferedReader(Charsets.UTF_8).readText()

        println("[XkcdRssProducer] XML primit: ${xml.length} caractere")
        return xml
    }
}
