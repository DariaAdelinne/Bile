package com.sd.laborator

import java.net.InetAddress
import java.net.Socket

/**
 * StudentMicroservice - un client de chat care se conecteaza la MessageManager si trimite
 * un mesaj. Se leaga de pe un PORT SURSA FIX (dat ca argument), pentru ca filtrarea dupa
 * gama de porturi (din FilterProcessor) sa fie determinista si usor de demonstrat.
 *
 * SOLID: S - singura responsabilitate: trimite un mesaj in chat de pe un port dat.
 */
class StudentMicroservice(
    private val name: String,
    private val sourcePort: Int,
    private val message: String
) {
    companion object {
        val MM_HOST: String = System.getenv("MESSAGE_MANAGER_HOST") ?: "localhost"
        const val MM_PORT = 1500
    }

    fun run() {
        // Socket(host, port, localAddr, localPort) -> leaga portul SURSA local la sourcePort
        val socket = Socket(
            InetAddress.getByName(MM_HOST), MM_PORT,
            InetAddress.getLoopbackAddress(), sourcePort
        )
        println("[$name] Conectat la MessageManager de pe portul sursa $sourcePort")

        socket.getOutputStream().write("MSG $message\n".toByteArray())
        socket.getOutputStream().flush()
        println("[$name] Trimis: \"$message\"")

        // las putin timp ca mesajul sa fie difuzat si procesat de FilterProcessor
        Thread.sleep(1500)
        socket.close()
        println("[$name] Inchis.")
    }
}

fun main(args: Array<String>) {
    val name = args.getOrElse(0) { "Student" }
    val sourcePort = args.getOrElse(1) { "6001" }.toInt()
    val message = args.getOrElse(2) { "Salut din chat" }
    StudentMicroservice(name, sourcePort, message).run()
}
