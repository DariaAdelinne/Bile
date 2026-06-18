import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import kotlin.system.exitProcess

class BiddingProcessorMicroservice {
    private var biddingProcessorSocket: ServerSocket
    private lateinit var auctioneerSocket: Socket
    private var receiveProcessedBidsObservable: Observable<String>
    private val subscriptions = CompositeDisposable()
    private val processedBidsQueue: Queue<Message> = LinkedList<Message>()

    companion object {
        const val BIDDING_PROCESSOR_PORT = 1700
        const val AUCTIONEER_PORT = 1500
        const val AUCTIONEER_HOST = "localhost"
    }

    init {
        biddingProcessorSocket = ServerSocket(BIDDING_PROCESSOR_PORT)
        println("BiddingProcessorMicroservice se executa pe portul: ${biddingProcessorSocket.localPort}")
        println("Se asteapta ofertele pentru finalizarea licitatiei...")

        val messageProcessorConnection = biddingProcessorSocket.accept()
        val bufferReader = BufferedReader(InputStreamReader(messageProcessorConnection.inputStream))

        receiveProcessedBidsObservable = Observable.create<String> { emitter ->
            while (true) {
                val receivedMessage = bufferReader.readLine()
                if (receivedMessage == null) {
                    bufferReader.close()
                    messageProcessorConnection.close()
                    emitter.onError(Exception("Eroare: MessageProcessorMicroservice ${messageProcessorConnection.port} a fost deconectat."))
                    break
                }
                if (Message.deserialize(receivedMessage.toByteArray()).body == "final") {
                    emitter.onComplete()
                    val finishedBidsMessage = Message.create(
                        "${messageProcessorConnection.localAddress}:${messageProcessorConnection.localPort}", "am primit tot"
                    )
                    messageProcessorConnection.getOutputStream().write(finishedBidsMessage.serialize())
                    messageProcessorConnection.close()
                    break
                } else {
                    emitter.onNext(receivedMessage)
                }
            }
        }
    }

    private fun receiveProcessedBids() {
        val receiveProcessedBidsSubscription = receiveProcessedBidsObservable
            .subscribeBy(
                onNext = {
                    val message = Message.deserialize(it.toByteArray())
                    println(message)
                    processedBidsQueue.add(message)
                },
                onComplete = { decideAuctionWinner() },
                onError = { println("Eroare: $it") }
            )
        subscriptions.add(receiveProcessedBidsSubscription)
    }

    private fun decideAuctionWinner() {
        val winner: Message? = processedBidsQueue.toList().maxByOrNull {
            it.body.split(" ")[1].toInt()
        }
        println("Castigatorul este: ${winner?.sender}")

        try {
            auctioneerSocket = Socket(AUCTIONEER_HOST, AUCTIONEER_PORT)
            auctioneerSocket.getOutputStream().write(winner!!.serialize())
            auctioneerSocket.close()
            println("Am anuntat castigatorul catre AuctioneerMicroservice.")
        } catch (e: Exception) {
            println("Nu ma pot conecta la Auctioneer!")
            biddingProcessorSocket.close()
            exitProcess(1)
        }
    }

    fun run() {
        receiveProcessedBids()
        subscriptions.dispose()
    }
}

fun main(args: Array<String>) {
    val biddingProcessorMicroservice = BiddingProcessorMicroservice()
    biddingProcessorMicroservice.run()
}
