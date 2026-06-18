package ro.exam.chatmaster

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import ro.exam.shared.ChatMessage
import ro.exam.shared.Message
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

/** Datele de rețea ale unei replici UserCommunicationMicroservice. */
data class UserEndpoint(val username: String, val host: String, val port: Int)

/** Cameră privată: numai membrii enumerați pot trimite și primi. */
data class PrivateRoom(val id: String, val members: Set<String>)

interface UserRegistry {
    fun register(endpoint: UserEndpoint)
    fun find(username: String): UserEndpoint?
}

class InMemoryUserRegistry : UserRegistry {
    private val users = ConcurrentHashMap<String, UserEndpoint>()
    override fun register(endpoint: UserEndpoint) { users[endpoint.username] = endpoint }
    override fun find(username: String): UserEndpoint? = users[username]
}

interface RoomRepository {
    fun save(room: PrivateRoom)
    fun find(roomId: String): PrivateRoom?
}

class InMemoryRoomRepository : RoomRepository {
    private val rooms = ConcurrentHashMap<String, PrivateRoom>()
    override fun save(room: PrivateRoom) { rooms[room.id] = room }
    override fun find(roomId: String): PrivateRoom? = rooms[roomId]
}

class PrivateConversationService(
    private val users: UserRegistry,
    private val rooms: RoomRepository
) {
    fun register(username: String, host: String, port: Int): String {
        require(username.isNotBlank()) { "Numele utilizatorului este gol" }
        require(port in 1..65535) { "Port invalid" }
        users.register(UserEndpoint(username, host, port))
        return "OK REGISTER $username"
    }

    fun createRoom(owner: String, roomId: String, requestedMembers: Set<String>): String {
        require(roomId.matches(Regex("[A-Za-z0-9_-]{2,40}"))) { "Nume cameră invalid" }
        val members = requestedMembers.map(String::trim).filter(String::isNotBlank).toMutableSet()
        members += owner
        require(members.size >= 2) { "Discuția privată necesită cel puțin 2 utilizatori" }
        val missing = members.filter { users.find(it) == null }
        require(missing.isEmpty()) { "Utilizatori neînregistrați: ${missing.joinToString(",")}" }
        rooms.save(PrivateRoom(roomId, members))
        return "OK ROOM $roomId membri=${members.sorted().joinToString(",")}" 
    }

    fun deliver(chat: ChatMessage): String {
        val room = rooms.find(chat.roomId) ?: throw IllegalArgumentException("Camera ${chat.roomId} nu există")
        require(chat.sender in room.members) { "Expeditorul nu este membru al camerei" }
        require(chat.text.isNotBlank()) { "Mesajul este gol" }
        require(chat.text.length <= 500) { "Mesaj prea lung" }

        val unavailable = mutableListOf<String>()
        room.members.forEach { username ->
            val endpoint = users.find(username)
            if (endpoint == null) {
                unavailable += username
            } else {
                runCatching {
                    Socket(endpoint.host, endpoint.port).use { socket ->
                        socket.getOutputStream().write(
                            Message.create("chat-master", "PRIVATE ${chat.serializeToText()}").serialize()
                        )
                        socket.getOutputStream().flush()
                    }
                }.onFailure { unavailable += username }
            }
        }
        require(unavailable.isEmpty()) { "Nu pot livra către: ${unavailable.joinToString(",")}" }
        return "OK DELIVERED ${room.members.size}"
    }
}

class ChatMasterMicroservice(
    private val port: Int = 1800,
    private val service: PrivateConversationService = PrivateConversationService(
        InMemoryUserRegistry(), InMemoryRoomRepository()
    )
) {
    fun run() {
        ServerSocket(port).use { server ->
            println("[ChatMaster] ascultă pe portul $port")
            Observable.create<Socket> { emitter ->
                while (!emitter.isDisposed) emitter.onNext(server.accept())
            }
                .subscribeOn(Schedulers.io())
                .flatMapCompletable { socket ->
                    io.reactivex.rxjava3.core.Completable.fromAction { handle(socket) }
                        .subscribeOn(Schedulers.io())
                }
                .blockingAwait()
        }
    }

    private fun handle(socket: Socket) = socket.use {
        val line = BufferedReader(InputStreamReader(it.getInputStream())).readLine()
        val request = Message.deserialize(line)
        val response = runCatching { process(request, it.inetAddress.hostAddress) }
            .fold({ result -> result }, { error -> "ERROR ${error.message}" })
        it.getOutputStream().write(Message.create("chat-master", response).serialize())
        it.getOutputStream().flush()
        println("[ChatMaster] ${request.sender}: $response")
    }

    private fun process(request: Message, remoteHost: String): String {
        val parts = request.body.split(' ', limit = 3)
        return when (parts.firstOrNull()) {
            "REGISTER" -> {
                require(parts.size >= 2) { "Format: REGISTER <port>" }
                service.register(request.sender, remoteHost, parts[1].toInt())
            }
            "CREATE" -> {
                require(parts.size == 3) { "Format: CREATE <camera> <membriCSV>" }
                service.createRoom(request.sender, parts[1], parts[2].split(',').toSet())
            }
            "DELIVER" -> {
                require(parts.size >= 2) { "Format: DELIVER <mesaj>" }
                service.deliver(ChatMessage.deserialize(request.body.substringAfter("DELIVER ")))
            }
            else -> throw IllegalArgumentException("Comandă necunoscută")
        }
    }
}

fun main() = ChatMasterMicroservice().run()
