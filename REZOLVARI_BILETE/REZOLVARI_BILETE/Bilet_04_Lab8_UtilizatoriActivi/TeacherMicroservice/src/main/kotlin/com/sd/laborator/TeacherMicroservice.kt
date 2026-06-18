package com.sd.laborator

import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import java.net.SocketTimeoutException
import kotlin.system.exitProcess

/**
 * TeacherMicroservice - MODIFICAT fata de laboratorul 8.
 *
 * Modificari:
 *  1. NU mai trimite intrebari prin MessageManagerMicroservice.
 *  2. Interogheaza ActiveUsersManagerMicroservice pentru a afla
 *     adresa (IP:port) unui student activ.
 *  3. Se conecteaza DIRECT la student pentru comunicare punctuala.
 *  4. Foloseste CORUTINE pentru operatiile de retea.
 *
 * Comenzi CLI:
 *   GETLIST              - afiseaza toti studentii activi
 *   VERIFICARE {name}    - verifica daca studentul e activ si obtine adresa sa
 *   INTREBARE {text}     - trimite o intrebare direct la studentul ales
 *   EXIT                 - inchide conexiunea cu studentul curent
 *   QUIT                 - iese din aplicatie
 *
 * Principii SOLID:
 *   S - singura responsabilitate: interfata CLI pentru profesor
 *   O - se pot adauga noi comenzi fara a modifica logica de baza
 *   D - depinde de ActiveUsersManager prin protocol TCP, nu prin referinta directa
 */
class TeacherMicroservice {
    private var currentStudentSocket: Socket? = null
    private var currentStudentName: String? = null

    companion object {
        val ACTIVE_USERS_HOST = System.getenv("ACTIVE_USERS_HOST") ?: "localhost"
        const val ACTIVE_USERS_PORT = 1500
        const val RESPONSE_TIMEOUT_MS = 3000
    }

    /**
     * Interogheaza ActiveUsersManager cu o comanda si returneaza raspunsul.
     * Suspend function - foloseste corutine.
     */
    private suspend fun queryActiveUsersManager(command: String): String =
        withContext(Dispatchers.IO) {
            try {
                Socket(ACTIVE_USERS_HOST, ACTIVE_USERS_PORT).use { socket ->
                    socket.getOutputStream().write((command + "\n").toByteArray())
                    socket.getOutputStream().flush()
                    BufferedReader(InputStreamReader(socket.inputStream)).readLine()
                        ?: "EROARE:RASPUNS_GOL"
                }
            } catch (e: Exception) {
                "EROARE:$e"
            }
        }

    /**
     * Trimite o intrebare direct la studentul curent conectat.
     * Suspend function - foloseste corutine.
     */
    private suspend fun sendDirectQuestion(question: String): String =
        withContext(Dispatchers.IO) {
            val socket = currentStudentSocket
            if (socket == null || socket.isClosed) {
                return@withContext "EROARE: Nu esti conectat la niciun student. Foloseste VERIFICARE {name}."
            }
            try {
                socket.soTimeout = RESPONSE_TIMEOUT_MS
                socket.getOutputStream().write((question + "\n").toByteArray())
                socket.getOutputStream().flush()
                BufferedReader(InputStreamReader(socket.inputStream)).readLine()
                    ?: "Studentul s-a deconectat."
            } catch (e: SocketTimeoutException) {
                "Nu a venit niciun raspuns in timp util ($RESPONSE_TIMEOUT_MS ms)."
            } catch (e: Exception) {
                "Eroare comunicare directa: $e"
            }
        }

    fun run() = runBlocking {
        println("=== TeacherMicroservice ===")
        println("Comenzi disponibile:")
        println("  GETLIST              - lista studenti activi")
        println("  VERIFICARE {name}    - verifica student si conecteaza-te direct")
        println("  INTREBARE {text}     - trimite intrebare direct la student")
        println("  EXIT                 - deconecteaza-te de la studentul curent")
        println("  QUIT                 - iesi din aplicatie")
        println()

        val stdin = BufferedReader(InputStreamReader(System.`in`))

        while (true) {
            val connectedTo = if (currentStudentName != null) "[conectat la: $currentStudentName]" else "[neconectat]"
            print("$connectedTo > ")
            System.out.flush()

            val input = withContext(Dispatchers.IO) { stdin.readLine() } ?: break
            val parts = input.trim().split(" ", limit = 2)
            val command = parts[0].uppercase()

            when (command) {

                "GETLIST" -> {
                    // Interogheaza ActiveUsersManager pentru lista utilizatorilor activi
                    val response = queryActiveUsersManager("GETLIST")
                    if (response == "LISTA_GOALA") {
                        println("Nu exista studenti activi.")
                    } else {
                        println("Studenti activi:")
                        response.lines().forEach { println("  $it") }
                    }
                }

                "VERIFICARE" -> {
                    if (parts.size < 2) {
                        println("Sintaxa: VERIFICARE {name}")
                        continue
                    }
                    val studentName = parts[1].trim()

                    // Inchide conexiunea anterioara daca exista
                    currentStudentSocket?.close()
                    currentStudentSocket = null
                    currentStudentName = null

                    // Intreaba ActiveUsersManager despre student
                    val response = queryActiveUsersManager("GETUSER $studentName")
                    println("ActiveUsersManager: $response")

                    if (response.startsWith("EROARE") || response == "INEXISTENT") {
                        println("Studentul '$studentName' nu este activ.")
                    } else {
                        // Raspuns de forma "host:port"
                        val (host, portStr) = response.split(":")
                        val port = portStr.toIntOrNull()
                        if (port == null) {
                            println("Format adresa invalid: $response")
                        } else {
                            // Initiaza conexiune DIRECTA cu studentul
                            try {
                                currentStudentSocket = withContext(Dispatchers.IO) {
                                    Socket(host, port)
                                }
                                currentStudentName = studentName
                                println("Conectat direct la $studentName @ $host:$port")
                                println("Acum poti folosi INTREBARE {text}")
                            } catch (e: Exception) {
                                println("Nu ma pot conecta direct la $studentName @ $host:$port: $e")
                            }
                        }
                    }
                }

                "INTREBARE" -> {
                    if (parts.size < 2) {
                        println("Sintaxa: INTREBARE {textul intrebarii}")
                        continue
                    }
                    val question = parts[1].trim()
                    // Trimite intrebarea direct la student (fara intermediar!)
                    val answer = sendDirectQuestion(question)
                    println("Raspuns de la $currentStudentName: $answer")
                }

                "EXIT" -> {
                    currentStudentSocket?.close()
                    currentStudentSocket = null
                    val prev = currentStudentName
                    currentStudentName = null
                    println("Deconectat de la $prev.")
                }

                "QUIT" -> {
                    println("Iesire...")
                    currentStudentSocket?.close()
                    exitProcess(0)
                }

                else -> println("Comanda necunoscuta: $command")
            }
        }
    }
}

fun main() {
    TeacherMicroservice().run()
}
