package org.example

import java.net.ServerSocket

class SocketLocal(port: Int) {
    private val serverSocket = ServerSocket(port)
    private val clientSocket = serverSocket.accept()

    fun send(data: String) {
        clientSocket.getOutputStream().write("$data\n".toByteArray())
    }

    fun close() {
        clientSocket.close()
        serverSocket.close()
    }
}
