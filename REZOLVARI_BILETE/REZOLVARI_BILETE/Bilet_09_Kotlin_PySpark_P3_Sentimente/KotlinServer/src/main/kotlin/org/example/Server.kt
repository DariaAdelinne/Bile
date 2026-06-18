package org.example

/**
 * Server TCP Kotlin - Problema 03
 *
 * 1. Preia stirile Apple (AAPL) din ultimele 2 saptamani de pe Finnhub
 * 2. Trimite fiecare stire prin socket TCP catre PySpark, cate una la 5 secunde
 *
 * PySpark face analiza de sentimente folosind fisierele positive/negative-words.txt
 */
fun main() {
    val API_TOKEN = "brmu7ovrh5r90ebn6jrg"
    val PORT = 8888

    println("Server pornit pe portul $PORT. Astept conexiunea PySpark...")
    val socket = SocketLocal(PORT)
    println("PySpark conectat. Incepe trimiterea stirilor Apple...")

    val api = Socket3rdParty(API_TOKEN)
    val news = api.getAppleNews()
    println("Stiri gasite: ${news.size}")

    for (newsItem in news) {
        Thread.sleep(5000)
        socket.send(newsItem.toString())
        println("Trimis: ${newsItem.optString("headline", "(fara titlu)")}")
    }

    println("Toate stirile au fost trimise.")
    socket.close()
}
