import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.*
import kotlin.system.exitProcess

class AuctioneerMicroservice {
    private var auctioneerSocket: ServerSocket
    private lateinit var messageProcessorSocket: Socket
    private var receiveBidsObservable: Observable<String>
    private val subscriptions = CompositeDisposable()
    private val bidQueue: Queue<Message> = LinkedList<Message>()
    private val bidderConnections: MutableList<Socket> = mutableListOf()

    companion object {
        const val MESSAGE_PROCESSOR_HOST = "localhost"
        const val MESSAGE_PROCESSOR_PORT = 1600
        const val AUCTIONEER_PORT = 1500
        const val AUCTION_DURATION: Long = 15_000
    }

    init {
        auctioneerSocket = ServerSocket(AUCTIONEER_PORT)
        auctioneerSocket.setSoTimeout(AUCTION_DURATION.toInt())
        println("AuctioneerMicroservice se executa pe portul: ${auctioneerSocket.localPort}")
        println("Se asteapta oferte de la bidderi...")

        receiveBidsObservable = Observable.create<String> { emitter ->
            while (true) {
                try {
                    val bidderConnection = auctioneerSocket.accept()
                    bidderConnections.add(bidderConnection)
                    val bufferReader = BufferedReader(InputStreamReader(bidderConnection.inputStream))
                    val receivedMessage = bufferReader.readLine()
                    if (receivedMessage == null) {
                        bufferReader.close()
                        bidderConnection.close()
                        emitter.onError(Exception("Eroare: Bidder-ul ${bidderConnection.port} a fost deconectat."))
                    }
                    emitter.onNext(receivedMessage)
                } catch (e: SocketTimeoutException) {
                    emitter.onComplete()
                    break
                }
            }
        }
    }

    private fun receiveBids() {
        val receiveBidsSubscription = receiveBidsObservable.subscribeBy(
            onNext = {
                val message = Message.deserialize(it.toByteArray())
                println(message)
                bidQueue.add(message)
            },
            onComplete = {
                println("Licitatia s-a incheiat! Se trimit ofertele spre procesare...")
                forwardBids()
            },
            onError = { println("Eroare: $it") }
        )
        subscriptions.add(receiveBidsSubscription)
    }

    private fun forwardBids() {
        try {
            messageProcessorSocket = Socket(MESSAGE_PROCESSOR_HOST, MESSAGE_PROCESSOR_PORT)
            subscriptions.add(Observable.fromIterable(bidQueue).subscribeBy(
                onNext = {
                    messageProcessorSocket.getOutputStream().write(it.serialize())
                    println("Am trimis mesajul: $it")
                },
                onComplete = {
                    println("Am trimis toate ofertele catre MessageProcessor.")
                    val bidEndMessage = Message.create(
                        "${messageProcessorSocket.localAddress}:${messageProcessorSocket.localPort}", "final"
                    )
                    messageProcessorSocket.getOutputStream().write(bidEndMessage.serialize())
                    val bufferReader = BufferedReader(InputStreamReader(messageProcessorSocket.inputStream))
                    bufferReader.readLine()
                    messageProcessorSocket.close()
                    finishAuction()
                }
            ))
        } catch (e: Exception) {
            println("Nu ma pot conecta la MessageProcessor!")
            auctioneerSocket.close()
            exitProcess(1)
        }
    }

    private fun finishAuction() {
        try {
            val biddingProcessorConnection = auctioneerSocket.accept()
            val bufferReader = BufferedReader(InputStreamReader(biddingProcessorConnection.inputStream))
            val receivedMessage = bufferReader.readLine()
            val result: Message = Message.deserialize(receivedMessage.toByteArray())
            val winningPrice = result.body.split(" ")[1].toInt()
            println("Castigator: ${result.sender} cu pretul: $winningPrice")

            val winningMessage = Message.create(auctioneerSocket.localSocketAddress.toString(), "Licitatie castigata! Pret castigator: $winningPrice")
            val losingMessage = Message.create(auctioneerSocket.localSocketAddress.toString(), "Licitatie pierduta...")

            bidderConnections.forEach {
                if (it.remoteSocketAddress.toString() == result.sender) {
                    it.getOutputStream().write(winningMessage.serialize())
                } else {
                    it.getOutputStream().write(losingMessage.serialize())
                }
                it.close()
            }
        } catch (e: Exception) {
            println("Nu ma pot conecta la BiddingProcessor!")
            auctioneerSocket.close()
            exitProcess(1)
        }
        subscriptions.dispose()
    }

    fun run() {
        receiveBids()
    }
}

fun main(args: Array<String>) {
    val auctioneerMicroservice = AuctioneerMicroservice()
    auctioneerMicroservice.run()
}
