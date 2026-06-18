package com.sd.laborator

import java.io.IOException
import java.net.Socket

/**
 * ErrorReporter - colaborator al MessageManager-ului: transporta evenimentele de eroare TCP
 * catre procesorul de flux (ErrorStatsProcessor) printr-un socket TCP persistent.
 *
 * Format mesaj:  "ERR <tip> <portSursa> <detaliu>\n"
 *
 * Se conecteaza LENES (la prima eroare) si se RECONECTEAZA daca legatura cade, ca MessageManager
 * sa poata porni si fara procesor (atunci erorile doar se ignora, nu blocheaza chat-ul).
 *
 * SOLID(S): singura responsabilitate = transportul evenimentelor de eroare catre procesor.
 * SOLID(D): MessageManager depinde de aceasta abstractizare, nu de socketul concret.
 */
class ErrorReporter {
    companion object {
        val PROCESSOR_HOST: String = System.getenv("ERROR_PROCESSOR_HOST") ?: "localhost"
        const val PROCESSOR_PORT = 1600
    }

    private var socket: Socket? = null

    @Synchronized
    private fun ensureConnected(): Socket? {
        val s = socket
        if (s != null && !s.isClosed) return s
        return try {
            val ns = Socket(PROCESSOR_HOST, PROCESSOR_PORT)
            socket = ns
            println("[ErrorReporter] Conectat la ErrorStatsProcessor $PROCESSOR_HOST:$PROCESSOR_PORT")
            ns
        } catch (e: IOException) {
            println("[ErrorReporter] Procesorul de flux nu e disponibil (${e.message}); eroarea se pierde.")
            null
        }
    }

    @Synchronized
    fun report(type: String, sourcePort: Int, detail: String) {
        val safeDetail = detail.replace("\n", " ").replace("\r", " ")
        val s = ensureConnected() ?: return
        try {
            s.getOutputStream().write("ERR $type $sourcePort $safeDetail\n".toByteArray())
            s.getOutputStream().flush()
        } catch (e: IOException) {
            // legatura catre procesor a cazut; o reseteaza si reincearca data viitoare
            socket = null
            println("[ErrorReporter] Trimitere esuata catre procesor: ${e.message}")
        }
    }
}
