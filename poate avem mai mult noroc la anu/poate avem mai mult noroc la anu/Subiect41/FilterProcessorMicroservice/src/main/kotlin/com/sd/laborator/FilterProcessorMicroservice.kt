package com.sd.laborator

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.Socket

/**
 * FilterProcessorMicroservice - procesorul de flux NOU (cerinta biletului).
 *
 * Se conecteaza la MessageManager (ca abonat) si primeste TOATA comunicatia, etichetata cu
 * portul sursa: "MSG <portSursa> <text>". Filtreaza dupa gama de porturi [minPort, maxPort]
 * si scrie mesajele ACCEPTATE intr-un fisier local (filtered_messages.log).
 *
 * SOLID:
 *   S - singura responsabilitate: filtrarea comunicatiei dupa port + salvarea in fisier.
 *   O - regula de filtrare (intervalul) e configurabila fara a modifica logica.
 *   D - depinde de protocolul MessageManager (port + format), nu de implementarea lui.
 */
class FilterProcessorMicroservice(private val minPort: Int, private val maxPort: Int) {

    companion object {
        val MM_HOST: String = System.getenv("MESSAGE_MANAGER_HOST") ?: "localhost"
        const val MM_PORT = 1500
        const val OUTPUT_FILE = "filtered_messages.log"
    }

    fun run() {
        val socket = Socket(MM_HOST, MM_PORT)
        println("[FilterProcessor] Conectat la MessageManager. Accept porturi in [$minPort, $maxPort].")
        println("[FilterProcessor] Mesajele acceptate se scriu in $OUTPUT_FILE")

        val reader = BufferedReader(InputStreamReader(socket.inputStream))
        while (true) {
            val line = reader.readLine() ?: break
            // format: "MSG <portSursa> <text>"
            val parts = line.split(" ", limit = 3)
            if (parts[0] != "MSG" || parts.size < 3) continue

            val port = parts[1].toIntOrNull() ?: continue
            val text = parts[2]

            if (port in minPort..maxPort) {
                File(OUTPUT_FILE).appendText("port=$port | $text\n")
                println("[FilterProcessor] ACCEPTAT (port $port): \"$text\"")
            } else {
                println("[FilterProcessor] RESPINS (port $port in afara intervalului): \"$text\"")
            }
        }
        socket.close()
    }
}

fun main(args: Array<String>) {
    // intervalul de porturi: din argumente, din variabile de mediu, sau implicit 6000-6010
    val minPort = args.getOrNull(0)?.toIntOrNull()
        ?: System.getenv("FILTER_MIN_PORT")?.toIntOrNull() ?: 6000
    val maxPort = args.getOrNull(1)?.toIntOrNull()
        ?: System.getenv("FILTER_MAX_PORT")?.toIntOrNull() ?: 6010
    FilterProcessorMicroservice(minPort, maxPort).run()
}
