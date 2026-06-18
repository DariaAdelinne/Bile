package com.sd.laborator

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.result.Result
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.net.ssl.*

const val API_TOKEN = "brmu7ovrh5r90ebn6jrg"
const val FINNHUB_BASE = "https://finnhub.io/api/v1"
const val TCP_PORT = 9999
const val DELAY_MS = 5000L
const val MAX_SYMBOLS = 20

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

fun fetchTwoWeeksNews(symbol: String): List<NewsArticle> {
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val today = LocalDate.now()
    val twoWeeksAgo = today.minusWeeks(2)
    val dateFrom = twoWeeksAgo.format(fmt)
    val dateTo = today.format(fmt)

    return try {
        val url = "$FINNHUB_BASE/company-news?symbol=$symbol&from=$dateFrom&to=$dateTo&token=$API_TOKEN"
        val (_, _, result) = url.httpGet().allowRedirects(true).responseString()
        val text = when (result) {
            is Result.Success -> result.get()
            is Result.Failure -> return emptyList()
        }
        val arr = JSONArray(text)
        val articles = mutableListOf<NewsArticle>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            articles.add(
                NewsArticle(
                    symbol   = symbol,
                    category = obj.optString("category", ""),
                    datetime = obj.optLong("datetime", 0L),
                    headline = obj.optString("headline", ""),
                    id       = obj.optLong("id", 0L),
                    image    = obj.optString("image", ""),
                    related  = obj.optString("related", ""),
                    source   = obj.optString("source", ""),
                    summary  = obj.optString("summary", ""),
                    url      = obj.optString("url", "")
                )
            )
        }
        articles
    } catch (e: Exception) {
        println("Eroare la preluarea stirilor pentru $symbol: ${e.message}")
        emptyList()
    }
}

fun handleClient(client: Socket, symbols: List<String>) {
    println("Client conectat: ${client.inetAddress.hostAddress}:${client.port}")
    val writer = PrintWriter(client.getOutputStream(), true)

    for (symbol in symbols) {
        if (client.isClosed) break

        println("Preiau stirile din ultimele 2 saptamani pentru: $symbol")
        val articles = fetchTwoWeeksNews(symbol)

        if (articles.isEmpty()) {
            println("  Nicio stire pentru $symbol.")
        }

        for (article in articles) {
            if (client.isClosed) break
            val json = Json.encodeToString(article)
            println("  Trimit stire: ${article.headline.take(60)}...")
            writer.println(json)
            Thread.sleep(DELAY_MS)
        }
    }

    println("Toate stirile au fost trimise. Inchid conexiunea cu clientul.")
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
    println("FinnhubNewsTCPServer (Subiect 33) pornit pe portul $TCP_PORT. Astept conexiuni...")

    while (true) {
        val client = server.accept()
        Thread { handleClient(client, symbols) }.start()
    }
}
