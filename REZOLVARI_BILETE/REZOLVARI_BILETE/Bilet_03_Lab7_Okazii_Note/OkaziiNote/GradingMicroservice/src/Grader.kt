import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread

/**
 * GradingMicroservice - MICROSERVICIU NOU adaugat pentru bilet.
 *
 * Responsabilitate (principiul S din SOLID):
 *   Primeste evaluari (nota 1-5) de la bidderi dupa finalizarea licitatiei
 *   si le salveaza in fisierul local "result.txt".
 *
 * Functionare:
 *   - Asculta pe portul 2000
 *   - Accepta conexiuni multiple in paralel (cate un thread per bidder)
 *   - Fiecare mesaj primit (ex: "Persoana (Popescu Ion, ...) a dat nota 3")
 *     este scris in fisierul result.txt
 *   - Se inchide automat cand toti bidderii s-au deconectat
 *
 * Principii SOLID:
 *   S - singura responsabilitate: receptie + scriere evaluari in fisier
 *   O - poate fi extins pentru a trimite statistici fara a modifica logica de baza
 *   D - nu depinde de alte microservicii (primeste conexiuni, nu initiaza)
 */
class GradingMicroservice {
    private var gradingSocket = ServerSocket(LOGGER_PORT)
    private var socketsConnected = 0
    private val socketsCounterLock = ReentrantLock()
    private val logLock = ReentrantLock()

    companion object {
        const val LOGGER_PORT = 2000
        const val FILE_PATH = "result.txt"
    }

    init {
        val file = File(FILE_PATH)
        if (!file.exists()) file.createNewFile()
        println("GradingMicroservice: fisier de evaluari deschis la: ${file.absolutePath}")
        println("GradingMicroservice se executa pe portul: $LOGGER_PORT")
        println("Se asteapta evaluari de la bidderi...")

        try {
            while (true) {
                val senderSocket = gradingSocket.accept()
                thread { handleConnection(senderSocket) }

                socketsCounterLock.lock()
                socketsConnected++
                socketsCounterLock.unlock()
            }
        } catch (e: SocketException) {
            if (socketsConnected == 0) {
                println("GradingMicroservice: inchidere normala.")
            } else {
                println("GradingMicroservice: eroare socket - $e")
            }
        }
    }

    private fun handleConnection(senderSocket: Socket) {
        try {
            val bufferReader = BufferedReader(InputStreamReader(senderSocket.inputStream))
            while (true) {
                val receivedMessage = bufferReader.readLine() ?: break

                println("GradingMicroservice a primit: $receivedMessage")

                logLock.lock()
                try {
                    Files.write(
                        Paths.get(FILE_PATH),
                        (receivedMessage + "\n").toByteArray(),
                        StandardOpenOption.APPEND
                    )
                } finally {
                    logLock.unlock()
                }
            }
            bufferReader.close()
        } catch (e: Exception) {
            println("GradingMicroservice: exceptie la conexiune - $e")
        } finally {
            senderSocket.close()

            socketsCounterLock.lock()
            socketsConnected--
            val remaining = socketsConnected
            socketsCounterLock.unlock()

            println("GradingMicroservice: bidder deconectat. Conexiuni active: $remaining")

            if (remaining == 0) {
                println("GradingMicroservice: toti bidderii s-au deconectat. Inchidere.")
                gradingSocket.close()
            }
        }
    }
}

fun main(args: Array<String>) {
    GradingMicroservice()
}
