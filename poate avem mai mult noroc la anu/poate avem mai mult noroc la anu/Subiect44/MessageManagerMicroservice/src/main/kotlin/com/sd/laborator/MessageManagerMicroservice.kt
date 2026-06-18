package com.sd.laborator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections

/**
 * MessageManagerMicroservice - brokerul de chat din laboratorul 8 (exemplul Student-Teacher),
 * rescris cu CORUTINE.
 *
 * Profesorul pune intrebari, studentii raspund. Brokerul difuzeaza (broadcast) fiecare mesaj
 * catre toti ceilalti abonati, ETICHETAND mesajul cu PORTUL SURSA al expeditorului, astfel incat
 * procesorul de flux (FilterProcessor) sa poata filtra dupa numarul portului.
 *
 * Protocol:
 *   client  -> manager :  "ASK <text>"            (profesorul pune o intrebare)
 *                         "ANSWER <text>"         (studentul raspunde)
 *   manager -> abonati :  "ASK <portSursa> <text>"
 *                         "ANSWER <portSursa> <text>"
 *
 * Corutine: cate o corutina per client (launch(Dispatchers.IO)).
 * SOLID(S): singura responsabilitate = difuzarea mesajelor in chat.
 */
class MessageManagerMicroservice {
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
                    } catch (e: IOException) {
                        println("[MessageManager] Eroare broadcast catre ${socket.port}: ${e.message}")
                    }
                }
            }
        }
    }

    private fun handleClient(client: Socket) {
        val sourcePort = client.port
        println("[MessageManager] Participant conectat de pe portul sursa $sourcePort")
        val reader = BufferedReader(InputStreamReader(client.inputStream))
        try {
            while (true) {
                val line = reader.readLine() ?: break
                val parts = line.split(" ", limit = 2)
                when (parts[0]) {
                    "ASK", "ANSWER" -> {
                        val text = parts.getOrElse(1) { "" }
                        println("[MessageManager] ${parts[0]} de la $sourcePort: \"$text\" -> broadcast")
                        broadcast("${parts[0]} $sourcePort $text", exceptPort = sourcePort)
                    }
                    "QUIT" -> break
                    else -> println("[MessageManager] Mesaj ignorat de la $sourcePort: '$line'")
                }
            }
        } catch (e: IOException) {
            println("[MessageManager] Participant $sourcePort deconectat: ${e.message}")
        } finally {
            subscribers.remove(client)
            try { client.close() } catch (_: IOException) {}
            println("[MessageManager] Participant $sourcePort inchis.")
        }
    }

    fun run() = runBlocking {
        val serverSocket = ServerSocket(PORT)
        println("[MessageManager] Pornit pe portul $PORT (corutine). Astept conexiuni...")
        while (true) {
            val client = serverSocket.accept()
            subscribers.add(client)
            launch(Dispatchers.IO) { handleClient(client) }
        }
    }
}

fun main() {
    MessageManagerMicroservice().run()
}
