package ro.exam.rating

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import ro.exam.shared.Message
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/** Entitate de domeniu. */
data class Rating(val username: String, val score: Int, val timestampEpochMillis: Long)

interface RatingRepository { fun save(rating: Rating) }

class CsvRatingRepository(private val path: Path) : RatingRepository {
    private val lock = Any()
    override fun save(rating: Rating) {
        synchronized(lock) {
            path.parent?.let(Files::createDirectories)
            if (Files.notExists(path)) {
                Files.writeString(
                    path,
                    "timestampEpochMillis,username,rating\n",
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
                )
            }
            val safeName = rating.username.replace("\"", "\"\"")
            Files.writeString(
                path,
                "${rating.timestampEpochMillis},\"$safeName\",${rating.score}\n",
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            )
        }
    }
}

class RatingValidator {
    fun validate(username: String, score: Int) {
        require(username.isNotBlank()) { "Numele nu poate fi gol" }
        require(username.length <= 100) { "Numele este prea lung" }
        require(score in 1..5) { "Evaluarea trebuie să fie între 1 și 5" }
    }
}

class RatingApplicationService(
    private val repository: RatingRepository,
    private val validator: RatingValidator
) {
    fun submit(message: Message): String {
        val parts = message.body.split(' ', limit = 3)
        require(parts.size == 3 && parts[0] == "rate") { "Format: rate <scor> <nume>" }
        val score = parts[1].toIntOrNull() ?: throw IllegalArgumentException("Scorul nu este număr")
        val username = parts[2].trim()
        validator.validate(username, score)
        repository.save(Rating(username, score, message.timestampEpochMillis))
        return "OK evaluare salvata"
    }
}

class RatingMicroservice(
    private val port: Int = 1800,
    private val app: RatingApplicationService = RatingApplicationService(
        CsvRatingRepository(Path.of(System.getenv("RATINGS_FILE") ?: "data/ratings.csv")), RatingValidator()
    )
) {
    fun run() {
        ServerSocket(port).use { server ->
            println("[RatingService] ascultă pe $port; fișier=${System.getenv("RATINGS_FILE") ?: "data/ratings.csv"}")
            Observable.create<Socket> { emitter ->
                while (!emitter.isDisposed) emitter.onNext(server.accept())
            }
                .subscribeOn(Schedulers.io())
                .flatMapCompletable { socket ->
                    io.reactivex.rxjava3.core.Completable.fromAction { handle(socket) }.subscribeOn(Schedulers.io())
                }
                .blockingAwait()
        }
    }

    private fun handle(socket: Socket) = socket.use {
        val line = BufferedReader(InputStreamReader(it.getInputStream())).readLine()
        val response = runCatching { app.submit(Message.deserialize(line)) }
            .fold({ it }, { "ERROR ${it.message}" })
        it.getOutputStream().write(Message.create("rating-service", response).serialize())
        it.getOutputStream().flush()
        println("[RatingService] $response")
    }
}

fun main() = RatingMicroservice().run()
