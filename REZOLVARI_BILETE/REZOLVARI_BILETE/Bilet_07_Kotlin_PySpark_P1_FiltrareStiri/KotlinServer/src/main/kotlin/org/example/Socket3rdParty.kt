package org.example

import khttp.get
import org.json.JSONObject
import java.time.LocalDate

/**
 * Apeleaza API-ul Finnhub pentru:
 *  1. Lista simbolurilor companiilor (stock symbols - US exchange)
 *  2. Stirile din ziua precedenta pentru fiecare simbol
 */
class Socket3rdParty(private val token: String) {
    private val apiUrl = "https://finnhub.io/api/v1/"

    fun getSymbols(): List<JSONObject> {
        val url = "${apiUrl}stock/symbol?exchange=US&token=$token"
        return get(url).jsonArray.map { JSONObject(it.toString()) }
    }

    fun getNewsForSymbol(symbol: String): List<JSONObject> {
        val yesterday = LocalDate.now().minusDays(1).toString()
        val today = LocalDate.now().toString()
        val url = "${apiUrl}company-news?symbol=$symbol&from=$yesterday&to=$today&token=$token"
        return try {
            get(url).jsonArray.map { JSONObject(it.toString()) }
        } catch (e: Exception) {
            println("Eroare la preluarea stirilor pentru $symbol: ${e.message}")
            emptyList()
        }
    }
}
