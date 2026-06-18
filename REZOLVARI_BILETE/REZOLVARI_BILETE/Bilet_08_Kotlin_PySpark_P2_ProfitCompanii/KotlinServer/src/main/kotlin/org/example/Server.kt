package org.example

/**
 * Server TCP Kotlin - Problema 02
 *
 * 1. Preia lista simbolurilor companiilor de pe Finnhub
 * 2. Pentru fiecare simbol preia pretul tinta (price-target)
 * 3. Trimite datele prin socket TCP catre PySpark, cate o companie la 3 secunde
 *
 * Formatul JSON trimis contine: symbol, targetLow, targetMean, targetHigh etc.
 * PySpark calculeaza profitul mediu si filtreaza companiile cu profit > 40%.
 */
fun main() {
    val API_TOKEN = "brl7eb7rh5re1lvco7fg"
    val PORT = 8888

    println("Server pornit pe portul $PORT. Astept conexiunea PySpark...")
    val socket = SocketLocal(PORT)
    println("PySpark conectat. Incepe trimiterea datelor de pret tinta...")

    val api = Socket3rdParty(API_TOKEN)
    val symbols = api.getSymbols()
    println("Simboluri gasite: ${symbols.size}")

    var totalSent = 0
    for (symbolObj in symbols) {
        val symbol = symbolObj.optString("symbol", "")
        if (symbol.isEmpty()) continue

        val priceTarget = api.getPriceTarget(symbol) ?: continue

        Thread.sleep(3000)
        socket.send(priceTarget.toString())
        println("Trimis: $symbol -> targetMean=${priceTarget.optDouble("targetMean")}, targetLow=${priceTarget.optDouble("targetLow")}")
        totalSent++
    }

    println("Total companii trimise: $totalSent")
    socket.close()
}
