package com.sd.laborator

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.result.Result
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import javax.net.ssl.*

const val API_TOKEN = "brl7eb7rh5re1lvco7fg"
const val FINNHUB_BASE = "https://finnhub.io/api/v1"
const val TCP_PORT = 9999
const val DELAY_MS = 3000L
const val MAX_SYMBOLS = 30

fun fetchSymbols(exchange: String = "US"): List<String> {
    println("Se preiau simbolurile de pe bursa: $exchange ...")
    val url = "$FINNHUB_BASE/stock/symbol?exchange=$exchange&token=$API_TOKEN"
    val (_, _, result) = url.httpGet().allowRedirects(true).responseString()
    val text = when (result) {
        is Result.Success -> result.get()
        is Result.Failure -> {
            println("Eroare HTTP: ${result.getException().message}")
            return emptyList()
        }
    }
    val arr = JSONArray(text)
    val symbols = mutableListOf<String>()
    for (i in 0 until minOf(arr.length(), MAX_SYMBOLS)) {
        symbols.add(arr.getJSONObject(i).getString("symbol"))
    }
    println("Simboluri preluate: ${symbols.size}")
    return symbols
}

fun fetchPriceTarget(symbol: String): PriceTargetData? {
    return try {
        val url = "$FINNHUB_BASE/stock/price-target?symbol=$symbol&token=$API_TOKEN"
        val (_, _, result) = url.httpGet().allowRedirects(true).responseString()
        val text = when (result) {
            is Result.Success -> result.get()
            is Result.Failure -> return null
        }
        val obj = JSONObject(text)
        if (!obj.has("targetMean") || obj.isNull("targetMean")) return null
        val targetMean = obj.getDouble("targetMean")
        if (targetMean == 0.0) return null
        PriceTargetData(
            symbol = symbol,
            targetHigh = obj.optDouble("targetHigh", 0.0),
            targetLow = obj.optDouble("targetLow", 0.0),
            targetMean = targetMean,
            targetMedian = obj.optDouble("targetMedian", 0.0),
            lastUpdated = obj.optString("lastUpdated", "N/A")
        )
    } catch (e: Exception) {
        println("Eroare la $symbol: ${e.message}")
        null
    }
}

fun handleClient(client: Socket, symbols: List<String>) {
    println("Client conectat: ${client.inetAddress.hostAddress}:${client.port}")
    val writer = PrintWriter(client.getOutputStream(), true)

    for (symbol in symbols) {
        if (client.isClosed) break

        val data = fetchPriceTarget(symbol)
        if (data != null) {
            val json = Json.encodeToString(data)
            println("Trimit: $json")
            writer.println(json)
        } else {
            println("Nu exista date pentru $symbol, trec mai departe.")
        }

        Thread.sleep(DELAY_MS)
    }

    println("Toate datele au fost trimise. Inchid conexiunea cu clientul.")
    client.close()
}

fun main() {
    val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(c: Array<java.security.cert.X509Certificate>, a: String) {}
        override fun checkServerTrusted(c: Array<java.security.cert.X509Certificate>, a: String) {}
        override fun getAcceptedIssuers() = arrayOf<java.security.cert.X509Certificate>()
    })
    val sc = SSLContext.getInstance("SSL")
    sc.init(null, trustAll, java.security.SecureRandom())
    FuelManager.instance.socketFactory = sc.socketFactory
    FuelManager.instance.hostnameVerifier = HostnameVerifier { _, _ -> true }

    val symbols = fetchSymbols()
    if (symbols.isEmpty()) {
        println("Nu s-au putut prelua simboluri. Verifica token-ul API.")
        return
    }

    val server = ServerSocket(TCP_PORT)
    println("FinnhubTCPServer pornit pe portul $TCP_PORT. Astept conexiuni...")

    while (true) {
        val client = server.accept()
        Thread { handleClient(client, symbols) }.start()
    }
}
