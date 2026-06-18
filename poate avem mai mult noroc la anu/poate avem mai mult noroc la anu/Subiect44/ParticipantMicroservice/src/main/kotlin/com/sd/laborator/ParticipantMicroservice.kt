package com.sd.laborator

import java.net.InetAddress
import java.net.Socket

/**
 * ParticipantMicroservice - client de chat din exemplul Student-Teacher. Se conecteaza la
 * MessageManager de pe un PORT SURSA FIX (dat ca argument), pentru ca filtrarea dupa numarul
 * portului (din FilterProcessor) sa fie determinista si usor de demonstrat.
 *
 * Rol:
 *   teacher -> trimite "ASK <mesaj>"     (profesorul pune o intrebare)
 *   student -> trimite "ANSWER <mesaj>"  (studentul raspunde)
 *
 * SOLID(S): singura responsabilitate = participarea la chat (intrebare sau raspuns) de pe un port dat.
 */
class ParticipantMicroservice(
    private val name: String,
    private val sourcePort: Int,
    private val role: String,
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
        val verb = if (role.equals("teacher", ignoreCase = true)) "ASK" else "ANSWER"
        println("[$name] Conectat de pe portul sursa $sourcePort (rol=$role -> $verb)")

        socket.getOutputStream().write("$verb $message\n".toByteArray())
        socket.getOutputStream().flush()
        println("[$name] Trimis: \"$verb $message\"")

        // las putin timp ca mesajul sa fie difuzat, filtrat si salvat in BD
        Thread.sleep(1500)
        socket.getOutputStream().write("QUIT\n".toByteArray())
        socket.getOutputStream().flush()
        socket.close()
        println("[$name] Inchis.")
    }
}

fun main(args: Array<String>) {
    val name = args.getOrElse(0) { "Participant" }
    val sourcePort = args.getOrElse(1) { "6001" }.toInt()
    val role = args.getOrElse(2) { "student" }   // teacher | student
    val message = args.getOrElse(3) { "Mesaj implicit" }
    ParticipantMicroservice(name, sourcePort, role, message).run()
}
