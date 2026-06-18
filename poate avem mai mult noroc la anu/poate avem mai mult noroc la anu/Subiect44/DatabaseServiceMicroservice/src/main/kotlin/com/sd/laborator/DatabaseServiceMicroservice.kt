package com.sd.laborator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket

/**
 * DatabaseServiceMicroservice - MICROSERVICIUL DE ADAUGARE IN BAZA DE DATE (cerinta Problemei 27).
 *
 * Server TCP (port 1700) la care FilterProcessor trimite mesajele FILTRATE (acceptate):
 *     "STORE <tip> <portSursa> <text>"
 * Fiecare mesaj este INSERAT in baza de date SQLite (prin MessageRepository).
 *
 * Corutine: cate o corutina per producator conectat.
 * SOLID(S): singura responsabilitate = receptia mesajelor + delegarea persistentei catre repository.
 * SOLID(D): depinde de abstractizarea MessageRepository, nu de detaliile JDBC/SQL.
 */
class DatabaseServiceMicroservice {
    private val repository = MessageRepository(DB_URL)

    companion object {
        const val PORT = 1700
        val DB_URL: String = System.getenv("DB_URL") ?: "jdbc:sqlite:filtered_messages.db"
    }

    private fun handle(line: String) {
        // format: "STORE <tip> <portSursa> <text...>"
        val parts = line.split(" ", limit = 4)
        if (parts.size < 4 || parts[0] != "STORE") {
            println("[DatabaseService] Linie ignorata (format necunoscut): '$line'")
            return
        }
        val type = parts[1]
        val port = parts[2].toIntOrNull() ?: return
        val text = parts[3]

        val total = repository.insert(type, port, text)
        println("[DatabaseService] INSERAT: $type port=$port \"$text\"  [total randuri = $total]")
    }

    private fun readFrom(producer: Socket) {
        val reader = BufferedReader(InputStreamReader(producer.inputStream))
        try {
            while (true) {
                val line = reader.readLine() ?: break
                handle(line)
            }
        } catch (e: IOException) {
            println("[DatabaseService] Producator deconectat: ${e.message}")
        } finally {
            try { producer.close() } catch (_: IOException) {}
        }
    }

    fun run() = runBlocking {
        val server = ServerSocket(PORT)
        println("[DatabaseService] Pornit pe portul $PORT (corutine). Astept mesaje filtrate...")
        while (true) {
            val producer = server.accept()
            println("[DatabaseService] Producator conectat: ${producer.inetAddress.hostAddress}:${producer.port}")
            launch(Dispatchers.IO) { readFrom(producer) }
        }
    }
}

fun main() {
    DatabaseServiceMicroservice().run()
}
