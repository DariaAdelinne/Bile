package com.sd.laborator

import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket

/**
 * ActiveUsersManagerMicroservice - INLOCUIESTE MessageManagerMicroservice din lab.
 *
 * Responsabilitate (principiul S din SOLID):
 *   Mentine o lista de utilizatori activi (name -> host:port) si
 *   raspunde la interogari despre disponibilitatea acestora.
 *   NU mai rutreaza mesaje intre utilizatori (eliminat procesorul central).
 *
 * Protocol TCP (port 1500):
 *   REGISTER {name} {port}  -> "OK" sau "EROARE:EXISTA"
 *   GETUSER {name}          -> "{host}:{port}" sau "INEXISTENT"
 *   GETLIST                 -> "{name1} {host1} {port1}\n..." + "END"
 *   UNREGISTER {name}       -> "OK"
 *
 * Corutine: fiecare conexiune client e tratata intr-o coroutina separata
 * (in loc de thread {} din laboratorul 8).
 *
 * Principii SOLID:
 *   S - singura responsabilitate: registru utilizatori activi
 *   O - se pot adauga noi comenzi fara a modifica logica existenta
 *   D - nu depinde de alte microservicii
 */

data class UserInfo(val host: String, val port: Int)

class ActiveUsersManagerMicroservice {
    private val activeUsers: HashMap<String, UserInfo> = hashMapOf()
    private val serverSocket = ServerSocket(PORT)

    companion object {
        const val PORT = 1500
    }

    fun run() = runBlocking {
        println("ActiveUsersManagerMicroservice pornit pe portul $PORT")
        println("Asteapta inregistrari si interogari...")

        while (true) {
            val clientSocket = serverSocket.accept()
            // Corutina pentru fiecare conexiune client
            launch(Dispatchers.IO) {
                handleClient(clientSocket)
            }
        }
    }

    private suspend fun handleClient(socket: Socket) {
        val host = socket.inetAddress.hostAddress
        println("Conexiune noua de la $host:${socket.port}")

        try {
            val reader = BufferedReader(InputStreamReader(socket.inputStream))
            val writer = socket.getOutputStream()

            val line = reader.readLine() ?: return
            println("Comanda primita: $line")

            val parts = line.trim().split(" ", limit = 3)
            val command = parts[0].uppercase()

            val response = synchronized(activeUsers) {
                when (command) {
                    "REGISTER" -> {
                        if (parts.size < 3) {
                            "EROARE:FORMAT_INVALID"
                        } else {
                            val name = parts[1]
                            val port = parts[2].toIntOrNull() ?: -1
                            if (activeUsers.containsKey(name)) {
                                // Reinnoire inregistrare (reconectare)
                                activeUsers[name] = UserInfo(host, port)
                                "OK:REINNOIT"
                            } else {
                                activeUsers[name] = UserInfo(host, port)
                                println("Utilizator inregistrat: $name @ $host:$port")
                                "OK"
                            }
                        }
                    }

                    "GETUSER" -> {
                        if (parts.size < 2) {
                            "EROARE:FORMAT_INVALID"
                        } else {
                            val name = parts[1]
                            val info = activeUsers[name]
                            if (info != null) "${info.host}:${info.port}" else "INEXISTENT"
                        }
                    }

                    "GETLIST" -> {
                        if (activeUsers.isEmpty()) {
                            "LISTA_GOALA"
                        } else {
                            activeUsers.entries.joinToString("\n") { (name, info) ->
                                "$name ${info.host} ${info.port}"
                            }
                        }
                    }

                    "UNREGISTER" -> {
                        if (parts.size < 2) {
                            "EROARE:FORMAT_INVALID"
                        } else {
                            val name = parts[1]
                            if (activeUsers.remove(name) != null) {
                                println("Utilizator deconectat: $name")
                                "OK"
                            } else {
                                "EROARE:INEXISTENT"
                            }
                        }
                    }

                    else -> "EROARE:COMANDA_NECUNOSCUTA"
                }
            }

            writer.write((response + "\n").toByteArray())
            writer.flush()
        } catch (e: Exception) {
            println("Eroare la conexiunea cu $host: $e")
        } finally {
            socket.close()
        }
    }
}

fun main() {
    ActiveUsersManagerMicroservice().run()
}
