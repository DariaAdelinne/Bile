package org.example

import khttp.get
import org.json.JSONObject

/**
 * Apeleaza API-ul Finnhub pentru:
 *  1. Lista simbolurilor companiilor (stock symbols - US exchange)
 *  2. Pretul tinta (price-target) pentru fiecare simbol
 *
 * Nota: price-target returneaza un singur obiect JSON (nu array),
 * de forma: {"symbol":"AAPL","targetHigh":200,"targetLow":100,"targetMean":150,...}
 */
class Socket3rdParty(private val token: String) {
    private val apiUrl = "https://finnhub.io/api/v1/"

    fun getSymbols(): List<JSONObject> {
        val url = "${apiUrl}stock/symbol?exchange=US&token=$token"
        return get(url).jsonArray.map { JSONObject(it.toString()) }
    }

    fun getPriceTarget(symbol: String): JSONObject? {
        val url = "${apiUrl}stock/price-target?symbol=$symbol&token=$token"
        return try {
            val response = get(url)
            val obj = response.jsonObject
            // Verifica ca are campurile necesare si nu sunt zero
            if (obj.optDouble("targetLow", 0.0) > 0 && obj.optDouble("targetMean", 0.0) > 0) {
                obj
            } else null
        } catch (e: Exception) {
            println("Eroare price-target pentru $symbol: ${e.message}")
            null
        }
    }
}
