package ro.exam.auctioneer

import io.reactivex.rxjava3.core.Observable
import ro.exam.shared.Message
import ro.exam.shared.StatisticsClient
import ro.exam.shared.StatisticsEventType
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.Collections

class AuctioneerMicroservice(
    private val bidderPort: Int = 1500,
    private val resultPort: Int = 1501,
    private val messageProcessorHost: String = "localhost",
    private val messageProcessorPort: Int = 1600,
    private val auctionDurationMs: Int = 15_000,
    private val statistics: StatisticsClient = StatisticsClient()
) {
    private val bidderConnections = Collections.synchronizedList(mutableListOf<Socket>())
    private val bids = Collections.synchronizedList(mutableListOf<Message>())

    fun run() {
        statistics.record(StatisticsEventType.AUCTION_STARTED)
        val resultServer = ServerSocket(resultPort)
        val bidderServer = ServerSocket(bidderPort).apply { soTimeout = 500 }
        println("[Auctioneer] bidderi: $bidderPort; rezultat: $resultPort; durată: ${auctionDurationMs / 1000}s")

        receiveBidsObservable(bidderServer).blockingSubscribe(
            { message -> bids += message; println("[Auctioneer] primit: $message") },
            { error -> println("[Auctioneer] eroare: ${error.message}") }
        )
        bidderServer.close()
        println("[Auctioneer] licitația s-a încheiat; oferte=${bids.size}")
        forwardBids()
        val result = receiveResult(resultServer)
        notifyBidders(result)
        resultServer.close()
        Thread.sleep(300)
        statistics.record(StatisticsEventType.AUCTION_ADJUDICATED)
    }

    private fun receiveBidsObservable(server: ServerSocket): Observable<Message> = Observable.create { emitter ->
        val deadline = System.currentTimeMillis() + auctionDurationMs
        try {
            while (System.currentTimeMillis() < deadline && !emitter.isDisposed) {
                try {
                    val socket = server.accept()
                    bidderConnections += socket
                    val line = BufferedReader(InputStreamReader(socket.getInputStream())).readLine()
                    if (line != null) emitter.onNext(Message.deserialize(line))
                } catch (_: SocketTimeoutException) { }
            }
            if (!emitter.isDisposed) emitter.onComplete()
        } catch (e: Exception) { if (!emitter.isDisposed) emitter.onError(e) }
    }

    private fun forwardBids() {
        Socket(messageProcessorHost, messageProcessorPort).use { socket ->
            Observable.fromIterable(bids).blockingSubscribe {
                socket.getOutputStream().write(it.serialize())
                statistics.record(StatisticsEventType.BID_TO_MESSAGE_PROCESSOR)
            }
            socket.getOutputStream().write(Message.create("auctioneer", "final").serialize())
            socket.getOutputStream().flush()
            statistics.record(StatisticsEventType.BIDS_END_SENT)
            BufferedReader(InputStreamReader(socket.getInputStream())).readLine()
        }
    }

    private fun receiveResult(server: ServerSocket): Message {
        println("[Auctioneer] așteaptă rezultatul...")
        return server.accept().use { connection ->
            Message.deserialize(BufferedReader(InputStreamReader(connection.getInputStream())).readLine())
        }
    }

    private fun notifyBidders(result: Message) {
        val winnerSender = if (result.body.startsWith("WIN ")) result.sender else null
        val price = result.body.substringAfter("WIN ", "-")
        bidderConnections.forEach { socket ->
            runCatching {
                val identity = "${socket.inetAddress.hostAddress}:${socket.port}"
                val body = if (winnerSender == identity) "Licitatie castigata! Pret: $price" else "Licitatie pierduta."
                socket.getOutputStream().write(Message.create("auctioneer", body).serialize())
                socket.getOutputStream().flush()
                statistics.record(StatisticsEventType.RESULT_TO_BIDDER)
                socket.close()
            }
        }
        println("[Auctioneer] toți bidderii au fost anunțați")
    }
}

fun main() = AuctioneerMicroservice().run()
