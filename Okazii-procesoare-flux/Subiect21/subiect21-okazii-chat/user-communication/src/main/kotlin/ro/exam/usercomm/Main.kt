package ro.exam.usercomm

import ro.exam.shared.ChatMessage
import ro.exam.shared.Message
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import kotlin.concurrent.thread

interface MasterClient {
    fun register(username: String, listenPort: Int): String
    fun createRoom(owner: String, roomId: String, members: String): String
}

class TcpMasterClient(
    private val host: String = "localhost",
    private val port: Int = 1800
) : MasterClient {
    override fun register(username: String, listenPort: Int): String = request(username, "REGISTER $listenPort")
    override fun createRoom(owner: String, roomId: String, members: String): String =
        request(owner, "CREATE $roomId $members")

    private fun request(sender: String, body: String): String = Socket(host, port).use { socket ->
        socket.getOutputStream().write(Message.create(sender, body).serialize())
        socket.getOutputStream().flush()
        Message.deserialize(BufferedReader(InputStreamReader(socket.getInputStream())).readLine()).body
    }
}

interface StreamChatClient { fun send(message: ChatMessage): String }

class TcpStreamChatClient(
    private val host: String = "localhost",
    private val port: Int = 1810
) : StreamChatClient {
    override fun send(message: ChatMessage): String = Socket(host, port).use { socket ->
        socket.getOutputStream().write(
            Message.create(message.sender, "CHAT ${message.serializeToText()}").serialize()
        )
        socket.getOutputStream().flush()
        Message.deserialize(BufferedReader(InputStreamReader(socket.getInputStream())).readLine()).body
    }
}

class UserCommunicationMicroservice(
    private val username: String,
    private val listenPort: Int,
    private val roomId: String?,
    private val members: String?,
    private val createRoom: Boolean,
    private val text: String?,
    private val createDelayMs: Long,
    private val sendDelayMs: Long,
    private val lifetimeMs: Long,
    private val master: MasterClient = TcpMasterClient(),
    private val stream: StreamChatClient = TcpStreamChatClient()
) {
    fun run() {
        val server = ServerSocket(listenPort).apply { soTimeout = 500 }
        val deadline = System.currentTimeMillis() + lifetimeMs
        val listener = thread(name = "listener-$username") {
            while (System.currentTimeMillis() < deadline) {
                try {
                    server.accept().use { connection -> receive(connection) }
                } catch (_: SocketTimeoutException) {
                    // verifică din nou durata de viață
                }
            }
        }

        println("[$username/Communication] ${master.register(username, listenPort)}; ascultă pe $listenPort")

        if (createRoom) {
            require(roomId != null && members != null) { "--create necesită --room și --members" }
            Thread.sleep(createDelayMs)
            println("[$username/Communication] ${master.createRoom(username, roomId, members)}")
        }

        if (text != null) {
            require(roomId != null) { "--message necesită --room" }
            Thread.sleep(sendDelayMs)
            val response = stream.send(ChatMessage.create(roomId, username, text))
            println("[$username/Communication] trimitere: $response")
        }

        listener.join()
        server.close()
        println("[$username/Communication] oprit")
    }

    private fun receive(socket: Socket) {
        val message = Message.deserialize(BufferedReader(InputStreamReader(socket.getInputStream())).readLine())
        if (message.body.startsWith("PRIVATE ")) {
            val chat = ChatMessage.deserialize(message.body.substringAfter("PRIVATE "))
            println("[$username/PRIVATE ${chat.roomId}] ${chat.sender}: ${chat.text}")
        }
    }
}

data class Args(
    val name: String,
    val port: Int,
    val room: String?,
    val members: String?,
    val create: Boolean,
    val message: String?,
    val createDelay: Long,
    val sendDelay: Long,
    val lifetime: Long
)

private fun parseArgs(args: Array<String>): Args {
    fun value(flag: String): String? {
        val index = args.indexOf(flag)
        return if (index >= 0 && index + 1 < args.size) args[index + 1] else null
    }
    val name = value("--name") ?: error("Lipsește --name")
    val port = value("--port")?.toIntOrNull() ?: error("Lipsește --port")
    return Args(
        name = name,
        port = port,
        room = value("--room"),
        members = value("--members"),
        create = args.contains("--create"),
        message = value("--message"),
        createDelay = value("--create-delay-ms")?.toLongOrNull() ?: 1_500,
        sendDelay = value("--send-delay-ms")?.toLongOrNull() ?: 3_000,
        lifetime = value("--lifetime-ms")?.toLongOrNull() ?: 12_000
    )
}

fun main(args: Array<String>) {
    val a = parseArgs(args)
    UserCommunicationMicroservice(
        a.name, a.port, a.room, a.members, a.create, a.message,
        a.createDelay, a.sendDelay, a.lifetime
    ).run()
}
