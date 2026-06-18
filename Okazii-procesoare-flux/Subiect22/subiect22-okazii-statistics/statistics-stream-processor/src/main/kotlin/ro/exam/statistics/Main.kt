package ro.exam.statistics

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.subscribeBy
import ro.exam.shared.StatisticsEvent
import ro.exam.shared.StatisticsEventType
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

interface AuctionStatisticsRepository {
    fun save(summary: AuctionStatisticsSummary)
}

data class AuctionStatisticsSummary(
    val auctionId: String,
    val startedAt: Long,
    val adjudicatedAt: Long,
    val totalMessages: Int,
    val counts: Map<StatisticsEventType, Int>
)

class CsvAuctionStatisticsRepository(
    private val path: Path = Path.of(System.getenv("STATISTICS_FILE") ?: "data/auction-statistics.csv")
) : AuctionStatisticsRepository {
    private val lock = Any()

    override fun save(summary: AuctionStatisticsSummary) {
        synchronized(lock) {
            path.parent?.let(Files::createDirectories)
            if (Files.notExists(path)) {
                Files.writeString(
                    path,
                    "auctionId,startedAt,adjudicatedAt,totalMessages,bids,bidToMessageProcessor,bidsEnd,acks,processedBids,processedEnd,resultToAuctioneer,resultToBidders\n",
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
                )
            }
            fun count(type: StatisticsEventType) = summary.counts[type] ?: 0
            val line = listOf(
                summary.auctionId,
                summary.startedAt,
                summary.adjudicatedAt,
                summary.totalMessages,
                count(StatisticsEventType.BID_SENT),
                count(StatisticsEventType.BID_TO_MESSAGE_PROCESSOR),
                count(StatisticsEventType.BIDS_END_SENT),
                count(StatisticsEventType.ACK_TO_AUCTIONEER),
                count(StatisticsEventType.PROCESSED_BID_TO_BIDDING),
                count(StatisticsEventType.PROCESSED_END_SENT),
                count(StatisticsEventType.RESULT_TO_AUCTIONEER),
                count(StatisticsEventType.RESULT_TO_BIDDER)
            ).joinToString(",") + "\n"
            Files.writeString(path, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
        }
    }
}

class AuctionStatisticsService(private val repository: AuctionStatisticsRepository) {
    private data class State(
        var startedAt: Long = Instant.now().toEpochMilli(),
        val counts: MutableMap<StatisticsEventType, Int> = ConcurrentHashMap()
    )

    private val states = ConcurrentHashMap<String, State>()

    fun accept(event: StatisticsEvent) {
        when (event.type) {
            StatisticsEventType.AUCTION_STARTED -> {
                states[event.auctionId] = State(startedAt = event.timestampEpochMillis)
                println("[Statistics] licitație începută: ${event.auctionId}")
            }
            StatisticsEventType.AUCTION_ADJUDICATED -> finish(event)
            else -> {
                val state = states.computeIfAbsent(event.auctionId) { State() }
                state.counts.merge(event.type, 1, Int::plus)
                println("[Statistics] ${event.type} -> ${state.counts[event.type]}")
            }
        }
    }

    private fun finish(event: StatisticsEvent) {
        val state = states.remove(event.auctionId) ?: State()
        val total = state.counts.entries
            .filter { it.key.countedMessage }
            .sumOf { it.value }
        val summary = AuctionStatisticsSummary(
            auctionId = event.auctionId,
            startedAt = state.startedAt,
            adjudicatedAt = event.timestampEpochMillis,
            totalMessages = total,
            counts = state.counts.toMap()
        )
        repository.save(summary)
        println("[Statistics] ADJUDECATĂ ${event.auctionId}; total mesaje=$total")
        println("[Statistics] salvat în data/auction-statistics.csv")
    }
}

class StatisticsStreamProcessorMicroservice(
    private val port: Int = 1800,
    private val service: AuctionStatisticsService = AuctionStatisticsService(CsvAuctionStatisticsRepository())
) {
    fun run() {
        ServerSocket(port).use { server ->
            println("[StatisticsStreamProcessor] ascultă pe portul $port")
            while (true) {
                val socket = server.accept()
                Observable.fromCallable {
                    socket.use {
                        val line = BufferedReader(InputStreamReader(it.getInputStream())).readLine()
                        StatisticsEvent.deserialize(line)
                    }
                }.subscribeBy(
                    onNext = service::accept,
                    onError = { println("[StatisticsStreamProcessor] eroare: ${it.message}") }
                )
            }
        }
    }
}

fun main() = StatisticsStreamProcessorMicroservice().run()
