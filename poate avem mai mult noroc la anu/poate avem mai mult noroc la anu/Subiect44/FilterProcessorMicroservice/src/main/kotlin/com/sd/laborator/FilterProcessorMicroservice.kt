package com.sd.laborator

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.Socket

/**
 * FilterProcessorMicroservice - PROCESORUL DE FLUX (cerinta Problemei 27).
 *
 * Se conecteaza la MessageManager (ca abonat) si primeste TOATA comunicatia, etichetata cu
 * portul sursa:  "ASK <portSursa> <text>" / "ANSWER <portSursa> <text>".
 * FILTREAZA dupa gama de porturi [minPort, maxPort]: mesajele ACCEPTATE sunt trimise mai departe
 * microserviciului de baza de date (DatabaseService) prin colaboratorul [DatabaseClient], sub forma
 *   "STORE <tip> <portSursa> <text>".
 *
 * SOLID:
 *   S - singura responsabilitate: filtrarea comunicatiei dupa port.
 *   O - intervalul de porturi e configurabil fara a modifica logica.
 *   D - depinde de protocolul MessageManager si de abstractizarea DatabaseClient, nu de SQL direct.
 */
class FilterProcessorMicroservice(private val minPort: Int, private val maxPort: Int) {

    private val dbClient = DatabaseClient()

    companion object {
        val MM_HOST: String = System.getenv("MESSAGE_MANAGER_HOST") ?: "localhost"
        const val MM_PORT = 1500
    }

    fun run() {
        val socket = Socket(MM_HOST, MM_PORT)
        println("[FilterProcessor] Conectat la MessageManager. Accept porturi in [$minPort, $maxPort].")
        println("[FilterProcessor] Mesajele acceptate se trimit la DatabaseService (port ${DatabaseClient.DB_PORT}).")

        val reader = BufferedReader(InputStreamReader(socket.inputStream))
        try {
            while (true) {
                val line = reader.readLine() ?: break
                // format: "<TIP> <portSursa> <text>" cu TIP in {ASK, ANSWER}
                val parts = line.split(" ", limit = 3)
                if (parts.size < 3 || (parts[0] != "ASK" && parts[0] != "ANSWER")) continue

                val type = parts[0]
                val port = parts[1].toIntOrNull() ?: continue
                val text = parts[2]

                if (port in minPort..maxPort) {
                    println("[FilterProcessor] ACCEPTAT ($type, port $port): \"$text\" -> BD")
                    dbClient.store(type, port, text)
                } else {
                    println("[FilterProcessor] RESPINS ($type, port $port in afara intervalului): \"$text\"")
                }
            }
        } catch (e: IOException) {
            println("[FilterProcessor] Deconectat de la MessageManager: ${e.message}")
        } finally {
            try { socket.close() } catch (_: IOException) {}
        }
    }
}

fun main(args: Array<String>) {
    val minPort = args.getOrNull(0)?.toIntOrNull()
        ?: System.getenv("FILTER_MIN_PORT")?.toIntOrNull() ?: 6000
    val maxPort = args.getOrNull(1)?.toIntOrNull()
        ?: System.getenv("FILTER_MAX_PORT")?.toIntOrNull() ?: 6010
    FilterProcessorMicroservice(minPort, maxPort).run()
}
