package ro.exam.biddingprocessor

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.subscribeBy
import ro.exam.shared.Message
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CountDownLatch

class BiddingProcessorMicroservice(
    private val listenPort: Int = 1700,
    private val auctioneerHost: String = "localhost",
    private val auctioneerResultPort: Int = 1501
) {
    fun run() {
        ServerSocket(listenPort).use { server ->
            println("[BiddingProcessor] ascultă pe portul $listenPort")
            server.accept().use { connection -> decide(connection) }
        }
    }

    private fun decide(connection: Socket) {
        val reader = BufferedReader(InputStreamReader(connection.getInputStream()))
        val bids = mutableListOf<Message>()
        val done = CountDownLatch(1)
        lineObservable(reader)
            .map(Message::deserialize)
            .takeUntil { it.body == "final" }
            .filter { it.body.startsWith("licitez ") }
            .subscribeBy(
                onNext = { bids += it; println("[BiddingProcessor] ofertă: $it") },
                onError = { println("[BiddingProcessor] eroare: ${it.message}"); done.countDown() },
                onComplete = {
                    val winner = bids.maxByOrNull { parseBid(it.body) }
                    sendWinner(winner)
                    done.countDown()
                }
            )
        done.await()
    }

    private fun parseBid(body: String): Int = body.substringAfter("licitez ").trim().toInt()

    private fun sendWinner(winner: Message?) {
        val result = if (winner == null) Message.create("bidding-processor", "NO_WINNER")
        else Message.create(winner.sender, "WIN ${parseBid(winner.body)}")
        Socket(auctioneerHost, auctioneerResultPort).use {
            it.getOutputStream().write(result.serialize()); it.getOutputStream().flush()
        }
        println("[BiddingProcessor] rezultat trimis: $result")
    }

    private fun lineObservable(reader: BufferedReader): Observable<String> = Observable.create { emitter ->
        try {
            while (!emitter.isDisposed) {
                val line = reader.readLine() ?: break
                emitter.onNext(line)
                if (runCatching { Message.deserialize(line).body == "final" }.getOrDefault(false)) break
            }
            if (!emitter.isDisposed) emitter.onComplete()
        } catch (e: Exception) { if (!emitter.isDisposed) emitter.onError(e) }
    }
}

fun main() = BiddingProcessorMicroservice().run()
