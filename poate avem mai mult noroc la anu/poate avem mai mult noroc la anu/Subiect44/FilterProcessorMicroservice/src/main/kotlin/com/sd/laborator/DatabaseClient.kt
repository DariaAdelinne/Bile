package com.sd.laborator

import java.io.IOException
import java.net.Socket

/**
 * DatabaseClient - colaborator al FilterProcessor-ului: transporta mesajele filtrate (acceptate)
 * catre microserviciul de baza de date (DatabaseService) printr-un socket TCP persistent.
 *
 * Format mesaj:  "STORE <tip> <portSursa> <text>\n"
 *
 * Se conecteaza LENES (la primul mesaj acceptat) si se RECONECTEAZA daca legatura cade.
 *
 * SOLID(S): singura responsabilitate = transportul mesajelor catre serviciul de BD.
 * SOLID(D): FilterProcessor depinde de aceasta abstractizare, nu de SQL/socketul concret.
 */
class DatabaseClient {
    companion object {
        val DB_HOST: String = System.getenv("DB_SERVICE_HOST") ?: "localhost"
        const val DB_PORT = 1700
    }

    private var socket: Socket? = null

    @Synchronized
    private fun ensureConnected(): Socket? {
        val s = socket
        if (s != null && !s.isClosed) return s
        return try {
            val ns = Socket(DB_HOST, DB_PORT)
            socket = ns
            println("[DatabaseClient] Conectat la DatabaseService $DB_HOST:$DB_PORT")
            ns
        } catch (e: IOException) {
            println("[DatabaseClient] DatabaseService indisponibil (${e.message}); mesajul nu se salveaza.")
            null
        }
    }

    @Synchronized
    fun store(type: String, sourcePort: Int, text: String) {
        val safeText = text.replace("\n", " ").replace("\r", " ")
        val s = ensureConnected() ?: return
        try {
            s.getOutputStream().write("STORE $type $sourcePort $safeText\n".toByteArray())
            s.getOutputStream().flush()
        } catch (e: IOException) {
            socket = null
            println("[DatabaseClient] Trimitere esuata catre DatabaseService: ${e.message}")
        }
    }
}
