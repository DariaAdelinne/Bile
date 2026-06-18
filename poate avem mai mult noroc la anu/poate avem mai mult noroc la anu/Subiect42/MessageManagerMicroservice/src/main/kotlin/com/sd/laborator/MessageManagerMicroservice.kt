package com.sd.laborator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.Collections

/**
 * MessageManagerMicroservice - brokerul de chat din laboratorul 8, rescris cu CORUTINE.
 *
 * Pe langa difuzarea mesajelor (broadcast), DETECTEAZA erorile de comunicare specifice TCP
 * care apar in dialogul cu clientii si le RAPORTEAZA procesorului de flux (ErrorStatsProcessor)
 * prin colaboratorul [ErrorReporter], sub forma:  "ERR <tip> <portSursa> <detaliu>".
 *
 * Tipuri de erori TCP detectate:
 *   CONNECTION_RESET  - clientul a inchis brusc conexiunea (RST) -> SocketException "Connection reset"
 *   BROKEN_PIPE       - scriere (broadcast) catre un socket deja inchis -> IOException
 *   MALFORMED_MESSAGE - mesaj care nu respecta protocolul (nu e "MSG"/"QUIT")
 *   EOF_UNEXPECTED    - clientul a inchis conexiunea (EOF) fara sa trimita "QUIT"
 *
 * Corutine: cate o corutina per client (launch(Dispatchers.IO)).
 * SOLID(S): singura responsabilitate = difuzarea mesajelor + semnalarea erorilor de comunicare.
 * SOLID(D): depinde de abstractizarea [ErrorReporter], nu de modul concret de transport al erorilor.
 */
class MessageManagerMicroservice {
    // toti clientii conectati la chat
    private val subscribers: MutableList<Socket> = Collections.synchronizedList(mutableListOf())
    private val reporter = ErrorReporter()

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
                        // scriere catre un socket inchis = eroare TCP "broken pipe"
                        reporter.report("BROKEN_PIPE", socket.port, e.message ?: "Broken pipe")
                        println("[MessageManager] BROKEN_PIPE catre ${socket.port}: ${e.message}")
                    }
                }
            }
        }
    }

    private fun handleClient(client: Socket) {
        val sourcePort = client.port
        println("[MessageManager] Client conectat de pe portul sursa $sourcePort")
        val reader = BufferedReader(InputStreamReader(client.inputStream))
        var graceful = false
        try {
            while (true) {
                val line = reader.readLine()
                if (line == null) {
                    // EOF: cealalta parte a inchis socketul. Daca NU a trimis QUIT -> eroare TCP.
                    if (!graceful) {
                        reporter.report("EOF_UNEXPECTED", sourcePort, "Conexiune inchisa fara QUIT")
                        println("[MessageManager] EOF_UNEXPECTED de la $sourcePort")
                    }
                    break
                }
                val parts = line.split(" ", limit = 2)
                when (parts[0]) {
                    "MSG" -> {
                        val text = parts.getOrElse(1) { "" }
                        println("[MessageManager] MSG de la $sourcePort: \"$text\" -> broadcast")
                        broadcast("MSG $sourcePort $text", exceptPort = sourcePort)
                    }
                    "QUIT" -> {
                        graceful = true
                        println("[MessageManager] $sourcePort a trimis QUIT (inchidere ordonata)")
                        break
                    }
                    else -> {
                        // mesaj care nu respecta protocolul -> eroare de comunicare
                        reporter.report("MALFORMED_MESSAGE", sourcePort, "Mesaj invalid: '$line'")
                        println("[MessageManager] MALFORMED_MESSAGE de la $sourcePort: '$line'")
                    }
                }
            }
        } catch (e: SocketException) {
            // tipic: "Connection reset" cand clientul rupe brusc conexiunea (RST)
            reporter.report("CONNECTION_RESET", sourcePort, e.message ?: "Connection reset")
            println("[MessageManager] CONNECTION_RESET de la $sourcePort: ${e.message}")
        } catch (e: IOException) {
            reporter.report("IO_ERROR", sourcePort, e.message ?: "IO error")
            println("[MessageManager] IO_ERROR de la $sourcePort: ${e.message}")
        } finally {
            subscribers.remove(client)
            try { client.close() } catch (_: IOException) {}
            println("[MessageManager] Client $sourcePort deconectat.")
        }
    }

    fun run() = runBlocking {
        val serverSocket = ServerSocket(PORT)
        println("[MessageManager] Pornit pe portul $PORT (corutine). Astept conexiuni...")
        println("[MessageManager] Erorile TCP se raporteaza la ErrorStatsProcessor (port ${ErrorReporter.PROCESSOR_PORT}).")
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
