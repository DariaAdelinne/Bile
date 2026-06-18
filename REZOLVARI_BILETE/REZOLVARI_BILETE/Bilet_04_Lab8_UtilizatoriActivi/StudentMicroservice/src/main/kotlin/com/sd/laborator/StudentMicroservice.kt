package com.sd.laborator

import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import kotlin.system.exitProcess

/**
 * StudentMicroservice - MODIFICAT fata de laboratorul 8.
 *
 * Modificari:
 *  1. NU mai foloseste MessageManagerMicroservice ca intermediar.
 *  2. La pornire se inregistreaza la ActiveUsersManagerMicroservice cu
 *     numele si portul sau.
 *  3. Porneste un ServerSocket pe un PORT DINAMIC (ales de OS) pentru
 *     a accepta conexiuni directe de la teacheri.
 *  4. Foloseste CORUTINE (launch/runBlocking) in loc de thread {}.
 *
 * Principii SOLID:
 *   S - singura responsabilitate: raspunde la intrebari din baza de date
 *   D - depinde de interfata ActiveUsersManager (port + protocol), nu de o impl. concreta
 */
class StudentMicroservice(private val name: String) {
    private lateinit var studentSocket: ServerSocket
    private val questionDatabase: MutableList<Pair<String, String>> = mutableListOf()

    companion object {
        val ACTIVE_USERS_HOST = System.getenv("ACTIVE_USERS_HOST") ?: "localhost"
        const val ACTIVE_USERS_PORT = 1500
    }

    init {
        // Incarca baza de date cu intrebari si raspunsuri
        val dbFile = File("questions_database.txt")
        if (dbFile.exists()) {
            val lines = dbFile.readLines()
            for (i in 0 until lines.size - 1 step 2) {
                questionDatabase.add(Pair(lines[i], lines[i + 1]))
            }
            println("[$name] Incarcat ${questionDatabase.size} perechi intrebare-raspuns.")
        } else {
            println("[$name] ATENTIE: questions_database.txt nu a fost gasit!")
        }
    }

    private fun registerAtActiveUsersManager(port: Int) {
        try {
            Socket(ACTIVE_USERS_HOST, ACTIVE_USERS_PORT).use { socket ->
                socket.getOutputStream().write("REGISTER $name $port\n".toByteArray())
                val response = BufferedReader(InputStreamReader(socket.inputStream)).readLine()
                println("[$name] Raspuns inregistrare: $response")
            }
        } catch (e: Exception) {
            println("[$name] Nu ma pot conecta la ActiveUsersManager! $e")
            exitProcess(1)
        }
    }

    private fun unregisterFromActiveUsersManager() {
        try {
            Socket(ACTIVE_USERS_HOST, ACTIVE_USERS_PORT).use { socket ->
                socket.getOutputStream().write("UNREGISTER $name\n".toByteArray())
            }
        } catch (e: Exception) {
            println("[$name] Nu ma pot deconecta de la ActiveUsersManager: $e")
        }
    }

    private fun findAnswer(question: String): String? {
        return questionDatabase.firstOrNull { it.first == question }?.second
    }

    /**
     * Trateaza o conexiune directa de la un teacher.
     * Foloseste corutina (suspend fun) - principiu corutine din cerinta.
     */
    private suspend fun handleDirectConnection(teacherSocket: Socket) {
        val teacherHost = teacherSocket.inetAddress.hostAddress
        println("[$name] Conexiune directa de la teacher $teacherHost:${teacherSocket.port}")

        try {
            val reader = BufferedReader(InputStreamReader(teacherSocket.inputStream))
            val writer = teacherSocket.getOutputStream()

            while (true) {
                val message = reader.readLine() ?: break
                println("[$name] Intrebare primita: \"$message\"")

                val answer = findAnswer(message)
                val response = answer ?: "Nu stiu raspunsul la aceasta intrebare."

                println("[$name] Trimit raspunsul: \"$response\"")
                writer.write((response + "\n").toByteArray())
                writer.flush()
            }
        } catch (e: Exception) {
            println("[$name] Eroare la conexiunea directa: $e")
        } finally {
            teacherSocket.close()
            println("[$name] Teacher $teacherHost s-a deconectat.")
        }
    }

    fun run() = runBlocking {
        // Porneste ServerSocket pe port dinamic (0 = ales de OS)
        studentSocket = ServerSocket(0)
        val myPort = studentSocket.localPort
        println("[$name] ServerSocket pornit pe portul $myPort")

        // Se inregistreaza la ActiveUsersManager
        registerAtActiveUsersManager(myPort)
        println("[$name] Inregistrat la ActiveUsersManager. Astept conexiuni directe...")

        // Coroutine pentru shutdown graceful
        Runtime.getRuntime().addShutdownHook(Thread {
            unregisterFromActiveUsersManager()
            println("[$name] Deconectat de la ActiveUsersManager.")
        })

        // Accepta conexiuni directe de la teacheri folosind corutine
        while (true) {
            val teacherSocket = withContext(Dispatchers.IO) {
                studentSocket.accept()
            }
            // Lanseaza o corutina separata pentru fiecare conexiune (in loc de thread {})
            launch(Dispatchers.IO) {
                handleDirectConnection(teacherSocket)
            }
        }
    }
}

fun main(args: Array<String>) {
    // Numele studentului poate fi dat ca argument sau generat automat
    val name = if (args.isNotEmpty()) args[0] else "Student_${(1..999).random()}"
    StudentMicroservice(name).run()
}
