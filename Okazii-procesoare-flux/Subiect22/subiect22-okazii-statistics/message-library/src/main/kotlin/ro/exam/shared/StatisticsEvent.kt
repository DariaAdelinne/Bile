package ro.exam.shared

import java.net.Socket
import java.nio.charset.StandardCharsets
import java.time.Instant

enum class StatisticsEventType(val countedMessage: Boolean) {
    AUCTION_STARTED(false),
    BID_SENT(true),
    BID_TO_MESSAGE_PROCESSOR(true),
    BIDS_END_SENT(true),
    ACK_TO_AUCTIONEER(true),
    PROCESSED_BID_TO_BIDDING(true),
    PROCESSED_END_SENT(true),
    RESULT_TO_AUCTIONEER(true),
    RESULT_TO_BIDDER(true),
    AUCTION_ADJUDICATED(false)
}

data class StatisticsEvent(
    val auctionId: String,
    val type: StatisticsEventType,
    val timestampEpochMillis: Long = Instant.now().toEpochMilli()
) {
    fun serialize(): ByteArray = "$timestampEpochMillis\t$auctionId\t${type.name}\n"
        .toByteArray(StandardCharsets.UTF_8)

    companion object {
        fun deserialize(line: String): StatisticsEvent {
            val parts = line.trim().split('\t')
            require(parts.size == 3) { "Eveniment statistic invalid" }
            return StatisticsEvent(parts[1], StatisticsEventType.valueOf(parts[2]), parts[0].toLong())
        }
    }
}

class StatisticsClient(
    private val host: String = "localhost",
    private val port: Int = 1800,
    private val auctionId: String = System.getenv("AUCTION_ID") ?: "licitatie-1"
) {
    fun record(type: StatisticsEventType) {
        runCatching {
            Socket(host, port).use { socket ->
                socket.getOutputStream().write(StatisticsEvent(auctionId, type).serialize())
                socket.getOutputStream().flush()
            }
        }.onFailure {
            System.err.println("[StatisticsClient] avertisment: ${it.message}")
        }
    }
}
