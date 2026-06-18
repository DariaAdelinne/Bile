package ro.exam.bidder

import ro.exam.shared.Message
import ro.exam.shared.StatisticsClient
import ro.exam.shared.StatisticsEventType
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import kotlin.random.Random

class BidderMicroservice(
    private val username: String,
    private val statistics: StatisticsClient = StatisticsClient(),
    private val auctioneerHost: String = "localhost",
    private val auctioneerPort: Int = 1500
) {
    fun run() {
        Socket(auctioneerHost, auctioneerPort).use { socket ->
            val identity = "${socket.localAddress.hostAddress}:${socket.localPort}"
            val bid = Random.nextInt(1_000, 10_001)
            val message = Message.create(identity, "licitez $bid $username")
            socket.getOutputStream().write(message.serialize())
            socket.getOutputStream().flush()
            statistics.record(StatisticsEventType.BID_SENT)
            println("[$username] ofertă=$bid; aștept rezultatul...")
            val result = Message.deserialize(BufferedReader(InputStreamReader(socket.getInputStream())).readLine())
            println("[$username] ${result.body}")
        }
    }
}

private fun parseName(args: Array<String>): String {
    val index = args.indexOf("--name")
    return if (index >= 0 && index + 1 < args.size) args[index + 1] else "utilizator-${Random.nextInt(1000, 9999)}"
}

fun main(args: Array<String>) = BidderMicroservice(parseName(args)).run()
