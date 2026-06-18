package ro.exam.bidder

import ro.exam.shared.Message
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import kotlin.random.Random

class RatingClient(private val host: String = "localhost", private val port: Int = 1800) {
    fun submit(username: String, score: Int): String {
        Socket(host, port).use { socket ->
            socket.getOutputStream().write(Message.create("bidder", "rate $score $username").serialize())
            socket.getOutputStream().flush()
            return Message.deserialize(BufferedReader(InputStreamReader(socket.getInputStream())).readLine()).body
        }
    }
}

class BidderMicroservice(
    private val username: String,
    private val requestedRating: Int?,
    private val auctioneerHost: String = "localhost",
    private val auctioneerPort: Int = 1500,
    private val ratingClient: RatingClient = RatingClient()
) {
    fun run() {
        Socket(auctioneerHost, auctioneerPort).use { socket ->
            val identity = "${socket.localAddress.hostAddress}:${socket.localPort}"
            val bid = Random.nextInt(1_000, 10_001)
            val message = Message.create(identity, "licitez $bid")
            socket.getOutputStream().write(message.serialize())
            // duplicat intenționat, pentru demonstrarea filtrării în MessageProcessor
            if (Random.nextBoolean()) socket.getOutputStream().write(message.serialize())
            socket.getOutputStream().flush()
            println("[$username] ofertă=$bid; aștept rezultatul...")
            val result = Message.deserialize(BufferedReader(InputStreamReader(socket.getInputStream())).readLine())
            println("[$username] ${result.body}")
        }
        val score = requestedRating ?: readRatingFromConsole()
        println("[$username] ${ratingClient.submit(username, score)}")
    }

    private fun readRatingFromConsole(): Int {
        while (true) {
            print("Evaluare serviciu (1-5): ")
            val score = readlnOrNull()?.trim()?.toIntOrNull()
            if (score in 1..5) return score!!
            println("Valoare invalidă. Introdu un număr între 1 și 5.")
        }
    }
}

private data class Args(val name: String, val rating: Int?)
private fun parseArgs(args: Array<String>): Args {
    fun value(flag: String): String? = args.indexOf(flag).takeIf { it >= 0 && it + 1 < args.size }?.let { args[it + 1] }
    val name = value("--name") ?: "utilizator-${Random.nextInt(1000, 9999)}"
    val rating = value("--rating")?.toIntOrNull()
    require(rating == null || rating in 1..5) { "--rating trebuie să fie între 1 și 5" }
    return Args(name, rating)
}

fun main(args: Array<String>) {
    val parsed = parseArgs(args)
    BidderMicroservice(parsed.name, parsed.rating).run()
}
