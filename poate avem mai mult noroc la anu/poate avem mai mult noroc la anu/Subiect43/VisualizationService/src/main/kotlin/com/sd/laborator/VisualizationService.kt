package com.sd.laborator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections

/**
 * VisualizationService - serviciul de VIZUALIZARE a elementelor citite (cerinta Problemei 08).
 *
 * Este backend-ul cu care comunica interfata grafica PyQt (prin TCP, port 1700). La cererea
 * "GET <oras>" intreaba serviciul de replicare (ReplicationService) si intoarce raspunsul catre GUI,
 * pastrand totodata un ISTORIC al elementelor citite (in memorie). La cererea "HISTORY" trimite
 * tot istoricul (cate o linie, terminat cu "END").
 *
 * Configurare ReplicationService: env REPLICATION_HOST / REPLICATION_PORT (implicit localhost:1500).
 *
 * Corutine: cate o corutina per client GUI conectat.
 * SOLID(S): singura responsabilitate = a media intre GUI si serviciul de replicare + a tine istoricul.
 */
class VisualizationService {
    private val history: MutableList<String> = Collections.synchronizedList(mutableListOf())

    companion object {
        const val PORT = 1700
        val REPL_HOST: String = System.getenv("REPLICATION_HOST") ?: "localhost"
        val REPL_PORT: Int = System.getenv("REPLICATION_PORT")?.toIntOrNull() ?: 1500
    }

    fun start() = runBlocking {
        val server = ServerSocket(PORT)
        println("[VisualizationService] Pornit pe portul $PORT. GUI-ul PyQt se conecteaza aici.")
        println("[VisualizationService] Sursa de date: ReplicationService $REPL_HOST:$REPL_PORT")
        while (true) {
            val client = server.accept()
            launch(Dispatchers.IO) { handle(client) }
        }
    }

    private fun handle(client: Socket) {
        val reader = BufferedReader(InputStreamReader(client.inputStream))
        val out = client.getOutputStream()
        try {
            while (true) {
                val line = reader.readLine() ?: break
                val parts = line.split(" ", limit = 2)
                when (parts[0]) {
                    "GET" -> {
                        val city = parts.getOrElse(1) { "" }.trim()
                        val resp = queryReplication(city)
                        if (resp.startsWith("OK")) {
                            history.add(resp)
                            println("[VisualizationService] Element citit pt. \"$city\": $resp")
                        } else {
                            println("[VisualizationService] Eroare pt. \"$city\": $resp")
                        }
                        out.write((resp + "\n").toByteArray()); out.flush()
                    }
                    "HISTORY" -> {
                        synchronized(history) {
                            history.forEach { out.write((it + "\n").toByteArray()) }
                        }
                        out.write("END\n".toByteArray()); out.flush()
                    }
                    else -> { out.write("ERR |comanda invalida\n".toByteArray()); out.flush() }
                }
            }
        } catch (e: IOException) {
            println("[VisualizationService] GUI deconectat: ${e.message}")
        } finally {
            try { client.close() } catch (_: IOException) {}
        }
    }

    private fun queryReplication(city: String): String {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(REPL_HOST, REPL_PORT), 2000)
                socket.soTimeout = 6000
                socket.getOutputStream().write("GET $city\n".toByteArray())
                socket.getOutputStream().flush()
                BufferedReader(InputStreamReader(socket.inputStream)).readLine()
                    ?: "ERR $city|raspuns gol de la replicare"
            }
        } catch (e: Exception) {
            "ERR $city|ReplicationService indisponibil (${e.message})"
        }
    }
}

fun main() {
    VisualizationService().start()
}
