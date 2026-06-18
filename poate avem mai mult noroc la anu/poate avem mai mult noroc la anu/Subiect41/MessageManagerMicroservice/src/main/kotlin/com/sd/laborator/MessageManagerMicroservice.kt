package com.sd.laborator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections

/**
 * MessageManagerMicroservice - brokerul de chat din laboratorul 8 (cu corutine).
 *
 * Primeste mesaje de la clienti si le difuzeaza (broadcast) catre toti ceilalti, ETICHETAND
 * fiecare mesaj cu PORTUL SURSA al expeditorului (portul de pe care s-a conectat clientul).
 * Astfel, procesorul de flux (FilterProcessor) poate filtra dupa gama de porturi.
 *
 * Protocol:
 *   client -> manager:  "MSG <text>"
 *   manager -> abonati: "MSG <portSursa> <text>"
 *
 * Corutine: cate o corutina per client (launch(Dispatchers.IO)).
 * SOLID: S - singura responsabilitate este difuzarea mesajelor.
 */
class MessageManagerMicroservice {
    // toti clientii conectati (inclusiv procesorul de filtrare, care doar asculta)
    private val subscribers: MutableList<Socket> = Collections.synchronizedList(mutableListOf())

    companion object {
        const val PORT = 1500
    }

    private fun broadcast(message: String, exceptPort: Int) {
        synchronized(subscribers) {
            subscribers.forEach { socket ->
                if (socket.port != exceptPort && !socket.isClosed) {
                    try {
                        socket.getOutputStream().write((message + "\n").toByteArray())
                        socket.getOutputStream().flush()
                    } catch (e: Exception) {
                        println("[MessageManager] Eroare broadcast catre ${socket.port}: ${e.message}")
                    }
                }
            }
        }
    }

    fun run() = runBlocking {
        val serverSocket = ServerSocket(PORT)
        println("[MessageManager] Pornit pe portul $PORT. Astept conexiuni...")

        while (true) {
            val client = serverSocket.accept()
            subscribers.add(client)
            launch(Dispatchers.IO) {
                val sourcePort = client.port  // portul SURSA al clientului
                println("[MessageManager] Client conectat de pe portul sursa $sourcePort")
                val reader = BufferedReader(InputStreamReader(client.inputStream))
                try {
                    while (true) {
                        val line = reader.readLine() ?: break
                        val parts = line.split(" ", limit = 2)
                        if (parts[0] == "MSG") {
                            val text = parts.getOrElse(1) { "" }
                            println("[MessageManager] MSG de la $sourcePort: \"$text\" -> broadcast")
                            // difuzeaza catre toti, etichetat cu portul sursa
                            broadcast("MSG $sourcePort $text", exceptPort = sourcePort)
                        }
                    }
                } catch (e: Exception) {
                    println("[MessageManager] Eroare client $sourcePort: ${e.message}")
                } finally {
                    subscribers.remove(client)
                    client.close()
                    println("[MessageManager] Client $sourcePort deconectat.")
                }
            }
        }
    }
}

fun main() {
    MessageManagerMicroservice().run()
}
