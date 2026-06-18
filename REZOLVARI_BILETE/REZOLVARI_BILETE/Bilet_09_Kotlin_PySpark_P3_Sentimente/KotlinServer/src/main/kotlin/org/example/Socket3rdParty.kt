package org.example

import khttp.get
import org.json.JSONObject
import java.time.LocalDate

/**
 * Apeleaza API-ul Finnhub pentru stirile Apple (AAPL) din ultimele 2 saptamani.
 */
class Socket3rdParty(private val token: String) {
    private val apiUrl = "https://finnhub.io/api/v1/"

    fun getAppleNews(): List<JSONObject> {
        val twoWeeksAgo = LocalDate.now().minusDays(14).toString()
        val today = LocalDate.now().toString()
        val url = "${apiUrl}company-news?symbol=AAPL&from=$twoWeeksAgo&to=$today&token=$token"
        return try {
            get(url).jsonArray.map { JSONObject(it.toString()) }
        } catch (e: Exception) {
            println("Eroare la preluarea stirilor Apple: ${e.message}")
            emptyList()
        }
    }
}
