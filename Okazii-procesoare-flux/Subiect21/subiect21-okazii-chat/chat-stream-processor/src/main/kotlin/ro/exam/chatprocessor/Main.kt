package ro.exam.chatprocessor

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import ro.exam.shared.ChatMessage
import ro.exam.shared.Message
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

interface ChatGateway { fun deliver(message: ChatMessage): String }

class TcpChatGateway(
    private val masterHost: String = "localhost",
    private val masterPort: Int = 1800
) : ChatGateway {
    override fun deliver(message: ChatMessage): String = Socket(masterHost, masterPort).use { socket ->
        socket.getOutputStream().write(
            Message.create("chat-stream-processor", "DELIVER ${message.serializeToText()}").serialize()
        )
        socket.getOutputStream().flush()
        Message.deserialize(BufferedReader(InputStreamReader(socket.getInputStream())).readLine()).body
    }
}

class ChatStreamProcessorMicroservice(
    private val port: Int = 1810,
    private val gateway: ChatGateway = TcpChatGateway()
) {
    private val processedIds = ConcurrentHashMap.newKeySet<String>()

    fun run() {
        ServerSocket(port).use { server ->
            println("[ChatStreamProcessor] ascultă pe portul $port")
            socketStream(server)
                .flatMapSingle { socket ->
                    io.reactivex.rxjava3.core.Single.fromCallable { process(socket) }
                        .subscribeOn(Schedulers.io())
                }
                .blockingSubscribe(
                    { println("[ChatStreamProcessor] $it") },
                    { println("[ChatStreamProcessor] eroare: ${it.message}") }
                )
        }
    }

    /** Flux reactiv de conexiuni, fiecare conexiune conținând un mesaj privat. */
    private fun socketStream(server: ServerSocket): Observable<Socket> = Observable.create { emitter ->
        while (!emitter.isDisposed) emitter.onNext(server.accept())
    }

    private fun process(socket: Socket): String = socket.use {
        val request = Message.deserialize(BufferedReader(InputStreamReader(it.getInputStream())).readLine())
        val response = runCatching {
            require(request.body.startsWith("CHAT ")) { "Format: CHAT <mesaj>" }
            val chat = ChatMessage.deserialize(request.body.substringAfter("CHAT "))
            require(processedIds.add(chat.id)) { "Mesaj duplicat eliminat" }
            require(chat.text.isNotBlank()) { "Mesaj gol" }
            val masterResponse = gateway.deliver(chat)
            require(masterResponse.startsWith("OK")) { masterResponse }
            "OK ${chat.roomId} ${chat.sender}"
        }.fold({ ok -> ok }, { error -> "ERROR ${error.message}" })

        it.getOutputStream().write(Message.create("chat-stream-processor", response).serialize())
        it.getOutputStream().flush()
        response
    }
}

fun main() = ChatStreamProcessorMicroservice().run()
