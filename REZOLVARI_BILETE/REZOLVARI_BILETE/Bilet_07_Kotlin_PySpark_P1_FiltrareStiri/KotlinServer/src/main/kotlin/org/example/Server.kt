package org.example

/**
 * Server TCP Kotlin - Problema 01
 *
 * 1. Preia lista simbolurilor companiilor de pe Finnhub (stock/symbol)
 * 2. Pentru fiecare simbol preia stirile din ziua precedenta (company-news)
 * 3. Trimite fiecare stire prin socket TCP catre clientul PySpark,
 *    cate una la 3 secunde, serialzata ca JSON pe o linie
 *
 * PySpark (main.py) se conecteaza pe portul 8888 si proceseaza streamul.
 */
fun main() {
    val API_TOKEN = "brmrfu7rh5rcss140ogg"
    val PORT = 8888

    println("Server pornit pe portul $PORT. Astept conexiunea PySpark...")
    val socket = SocketLocal(PORT)
    println("PySpark conectat. Incepe trimiterea stirilor...")

    val api = Socket3rdParty(API_TOKEN)
    val symbols = api.getSymbols()
    println("Simboluri gasite: ${symbols.size}")

    var totalSent = 0
    for (symbolObj in symbols) {
        val symbol = symbolObj.optString("symbol", "")
        if (symbol.isEmpty()) continue

        val news = api.getNewsForSymbol(symbol)
        for (newsItem in news) {
            Thread.sleep(3000)
            val json = newsItem.toString()
            socket.send(json)
            println("Trimis: ${newsItem.optString("headline", "(fara titlu)")}")
            totalSent++
        }
    }

    println("Total stiri trimise: $totalSent")
    socket.close()
}
