package ro.exam.messageprocessor

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.subscribeBy
import ro.exam.shared.Message
import ro.exam.shared.StatisticsClient
import ro.exam.shared.StatisticsEventType
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CountDownLatch

class MessageProcessorMicroservice(
    private val listenPort: Int = 1600,
    private val biddingHost: String = "localhost",
    private val biddingPort: Int = 1700,
    private val statistics: StatisticsClient = StatisticsClient()
) {
    fun run() {
        ServerSocket(listenPort).use { server ->
            println("[MessageProcessor] ascultă pe portul $listenPort")
            server.accept().use { auctioneer -> processConnection(auctioneer) }
        }
    }

    private fun processConnection(auctioneer: Socket) {
        val reader = BufferedReader(InputStreamReader(auctioneer.getInputStream()))
        val done = CountDownLatch(1)
        val processed = mutableListOf<Message>()
        lineObservable(reader)
            .map(Message::deserialize)
            .takeUntil { it.body == "final" }
            .filter { it.body != "final" }
            .distinct { it.uniqueKey() }
            .toList()
            .flattenAsObservable { it.sortedBy(Message::timestampEpochMillis) }
            .subscribeBy(
                onNext = { processed += it; println("[MessageProcessor] acceptat: $it") },
                onError = { println("[MessageProcessor] eroare: ${it.message}"); done.countDown() },
                onComplete = {
                    auctioneer.getOutputStream().write(Message.create("message-processor", "am primit tot").serialize())
                    auctioneer.getOutputStream().flush()
                    statistics.record(StatisticsEventType.ACK_TO_AUCTIONEER)
                    forward(processed)
                    done.countDown()
                }
            )
        done.await()
    }

    private fun forward(messages: List<Message>) {
        Socket(biddingHost, biddingPort).use { socket ->
            Observable.fromIterable(messages).blockingSubscribe {
                socket.getOutputStream().write(it.serialize())
                statistics.record(StatisticsEventType.PROCESSED_BID_TO_BIDDING)
            }
            socket.getOutputStream().write(Message.create("message-processor", "final").serialize())
            socket.getOutputStream().flush()
            statistics.record(StatisticsEventType.PROCESSED_END_SENT)
        }
        println("[MessageProcessor] toate ofertele procesate au fost trimise")
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

fun main() = MessageProcessorMicroservice().run()
