package com.sd.laborator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/**
 * ErrorStatsProcessorMicroservice - PROCESORUL DE FLUX (cerinta biletului).
 *
 * Este un server TCP (port 1600) la care MessageManager raporteaza erorile de comunicare TCP:
 *     "ERR <tip> <portSursa> <detaliu>"
 * Procesorul realizeaza STATISTICI pe tipuri de erori (cate de fiecare tip au aparut) si
 * TRIMITE fiecare eroare serviciului REST (HTTP POST /errors), care o adauga intr-un fisier XML
 * local si o afiseaza in browser.
 *
 * Corutine: cate o corutina per producator conectat (launch(Dispatchers.IO)).
 * SOLID(S): singura responsabilitate = agregarea statisticilor de erori + forwarding la REST.
 * SOLID(O): tipurile de erori nu sunt fixate in cod (orice tip nou e numarat automat).
 */
class ErrorStatsProcessorMicroservice {
    // statistici: tip eroare -> numar de aparitii
    private val stats = ConcurrentHashMap<String, Int>()

    companion object {
        const val PORT = 1600
        val REST_URL: String = System.getenv("ERROR_REST_URL") ?: "http://localhost:8080/errors"
    }

    private fun handle(line: String) {
        // format asteptat: "ERR <tip> <portSursa> <detaliu...>"
        val parts = line.split(" ", limit = 4)
        if (parts.size < 4 || parts[0] != "ERR") {
            println("[Processor] Linie ignorata (format necunoscut): '$line'")
            return
        }
        val type = parts[1]
        val sourcePort = parts[2]
        val detail = parts[3]

        val count = stats.merge(type, 1) { a, b -> a + b }!!
        println("[Processor] Eroare $type (port $sourcePort): \"$detail\"  [total $type = $count]")
        printStats()
        forwardToRest(type, sourcePort, detail, count)
    }

    private fun printStats() {
        val summary = stats.entries.sortedByDescending { it.value }
            .joinToString(", ") { "${it.key}=${it.value}" }
        println("[Processor] === STATISTICI erori: $summary ===")
    }

    private fun forwardToRest(type: String, sourcePort: String, detail: String, count: Int) {
        try {
            val body = "type=${enc(type)}&sourcePort=${enc(sourcePort)}&detail=${enc(detail)}&count=$count"
            val conn = (URL(REST_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connectTimeout = 3000
                readTimeout = 3000
            }
            OutputStreamWriter(conn.outputStream).use { it.write(body) }
            val code = conn.responseCode
            println("[Processor] Trimis la REST ($REST_URL) -> HTTP $code")
            conn.disconnect()
        } catch (e: IOException) {
            println("[Processor] Serviciul REST indisponibil (${e.message}); statistica se pastreaza local.")
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun readFrom(producer: Socket) {
        val reader = BufferedReader(InputStreamReader(producer.inputStream))
        try {
            while (true) {
                val line = reader.readLine() ?: break
                handle(line)
            }
        } catch (e: IOException) {
            println("[Processor] Producator deconectat: ${e.message}")
        } finally {
            try { producer.close() } catch (_: IOException) {}
        }
    }

    fun run() = runBlocking {
        val server = ServerSocket(PORT)
        println("[Processor] Procesor de flux pornit pe portul $PORT (corutine). Astept evenimente de eroare...")
        println("[Processor] Forwarding catre serviciul REST: $REST_URL")
        while (true) {
            val producer = server.accept()
            println("[Processor] Producator conectat: ${producer.inetAddress.hostAddress}:${producer.port}")
            launch(Dispatchers.IO) { readFrom(producer) }
        }
    }
}

fun main() {
    ErrorStatsProcessorMicroservice().run()
}
