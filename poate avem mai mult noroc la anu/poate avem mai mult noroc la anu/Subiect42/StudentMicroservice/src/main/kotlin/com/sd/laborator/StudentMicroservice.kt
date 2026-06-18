package com.sd.laborator

import java.net.InetAddress
import java.net.Socket

/**
 * StudentMicroservice - client de chat care se conecteaza la MessageManager de pe un PORT
 * SURSA FIX (ca erorile sa fie usor de urmarit dupa port). Are mai multe MODURI de comportare,
 * pentru a putea demonstra fiecare tip de eroare TCP pe care le numara procesorul de flux:
 *
 *   ok        - trimite "MSG ..." corect si inchide ordonat cu "QUIT"          (FARA eroare)
 *   malformed - trimite un mesaj fara prefixul protocolului                    -> MALFORMED_MESSAGE
 *   eof       - trimite "MSG ..." apoi inchide conexiunea fara "QUIT"          -> EOF_UNEXPECTED
 *   abrupt    - trimite "MSG ..." apoi rupe BRUSC socketul (RST, SO_LINGER 0)  -> CONNECTION_RESET
 *
 * SOLID(S): singura responsabilitate = simularea unui client de chat (eventual defect).
 */
class StudentMicroservice(
    private val name: String,
    private val sourcePort: Int,
    private val mode: String,
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
        println("[$name] Conectat de pe portul sursa $sourcePort (mod=$mode)")
        val out = socket.getOutputStream()

        when (mode) {
            "ok" -> {
                out.write("MSG $message\n".toByteArray()); out.flush()
                println("[$name] Trimis corect: \"$message\"")
                Thread.sleep(500)
                out.write("QUIT\n".toByteArray()); out.flush()
                Thread.sleep(300)
                socket.close()
                println("[$name] Inchis ordonat (QUIT) - fara eroare.")
            }
            "malformed" -> {
                // mesaj fara prefix MSG/QUIT -> MessageManager raporteaza MALFORMED_MESSAGE
                out.write("buna ziua fara protocol\n".toByteArray()); out.flush()
                println("[$name] Trimis mesaj MALFORMAT (fara prefix).")
                Thread.sleep(500)
                out.write("QUIT\n".toByteArray()); out.flush()
                Thread.sleep(300)
                socket.close()
                println("[$name] Inchis.")
            }
            "eof" -> {
                out.write("MSG $message\n".toByteArray()); out.flush()
                println("[$name] Trimis, apoi inchid FARA QUIT (EOF neasteptat).")
                Thread.sleep(400)
                socket.close()  // FIN normal, dar fara QUIT -> server vede EOF_UNEXPECTED
            }
            "abrupt" -> {
                out.write("MSG $message\n".toByteArray()); out.flush()
                println("[$name] Trimis, apoi RUP BRUSC conexiunea (RST).")
                Thread.sleep(400)
                // SO_LINGER 0 -> close() trimite RST in loc de FIN -> "Connection reset" la server
                socket.setSoLinger(true, 0)
                socket.close()
            }
            else -> {
                println("[$name] Mod necunoscut: '$mode'. Foloseste: ok | malformed | eof | abrupt")
                socket.close()
            }
        }
        println("[$name] Gata.")
    }
}

fun main(args: Array<String>) {
    val name = args.getOrElse(0) { "Student" }
    val sourcePort = args.getOrElse(1) { "6001" }.toInt()
    val mode = args.getOrElse(2) { "ok" }
    val message = args.getOrElse(3) { "Salut din chat" }
    StudentMicroservice(name, sourcePort, mode, message).run()
}
