import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import kotlin.system.exitProcess

/**
 * MessageProcessorMicroservice - TODO-urile din laborator sunt rezolvate:
 *  1. Filtrare duplicate (operatorul distinct() din RxJava)
 *  2. Sortare dupa timestamp (sortedBy { it.timestamp })
 *
 * Primeste ofertele de la Auctioneer, le filtreaza si sorteaza,
 * apoi le trimite la BiddingProcessorMicroservice.
 */
class MessageProcessorMicroservice {
    private var messageProcessorSocket: ServerSocket
    private lateinit var biddingProcessorSocket: Socket
    private var auctioneerConnection: Socket
    private var receiveInQueueObservable: Observable<String>
    private val subscriptions = CompositeDisposable()
    private var messageQueue: Queue<Message> = LinkedList<Message>()

    companion object {
        const val MESSAGE_PROCESSOR_PORT = 1600
        const val BIDDING_PROCESSOR_HOST = "localhost"
        const val BIDDING_PROCESSOR_PORT = 1700
    }

    init {
        messageProcessorSocket = ServerSocket(MESSAGE_PROCESSOR_PORT)
        println("MessageProcessorMicroservice se executa pe portul: ${messageProcessorSocket.localPort}")
        println("Se asteapta mesaje pentru procesare...")

        auctioneerConnection = messageProcessorSocket.accept()
        val bufferReader = BufferedReader(InputStreamReader(auctioneerConnection.inputStream))

        receiveInQueueObservable = Observable.create<String> { emitter ->
            while (true) {
                val receivedMessage = bufferReader.readLine()

                if (receivedMessage == null) {
                    bufferReader.close()
                    auctioneerConnection.close()
                    emitter.onError(Exception("AuctioneerMicroservice s-a deconectat."))
                    break
                }

                if (Message.deserialize(receivedMessage.toByteArray()).body == "final") {
                    emitter.onComplete()
                    break
                } else {
                    emitter.onNext(receivedMessage)
                }
            }
        }
    }

    private fun receiveAndProcessMessages() {
        val receiveInQueueSubscription = receiveInQueueObservable
            // === TODO REZOLVAT 1: Filtrare duplicate cu operatorul distinct() ===
            .distinct { Message.deserialize(it.toByteArray()).let { m -> "${m.sender}${m.body}" } }
            .subscribeBy(
                onNext = {
                    val message = Message.deserialize(it.toByteArray())
                    println("Primit (fara duplicate): $message")
                    messageQueue.add(message)
                },
                onComplete = {
                    // === TODO REZOLVAT 2: Sortare dupa timestamp ===
                    val sortedQueue = messageQueue.sortedBy { it.timestamp }
                    messageQueue = LinkedList<Message>(sortedQueue)
                    println("Mesaje dupa sortare cronologica:")
                    messageQueue.forEach { println("  $it") }

                    val finishedMsg = Message.create(
                        "${auctioneerConnection.localAddress}:${auctioneerConnection.localPort}",
                        "am primit tot"
                    )
                    auctioneerConnection.getOutputStream().write(finishedMsg.serialize())
                    auctioneerConnection.close()

                    sendProcessedMessages()
                },
                onError = { println("Eroare: $it") }
            )
        subscriptions.add(receiveInQueueSubscription)
    }

    private fun sendProcessedMessages() {
        try {
            biddingProcessorSocket = Socket(BIDDING_PROCESSOR_HOST, BIDDING_PROCESSOR_PORT)

            println("Trimit mesajele procesate catre BiddingProcessor:")
            Observable.fromIterable(messageQueue).subscribeBy(
                onNext = {
                    println("  -> $it")
                    biddingProcessorSocket.getOutputStream().write(it.serialize())
                },
                onComplete = {
                    val noMoreMessages = Message.create(
                        "${biddingProcessorSocket.localAddress}:${biddingProcessorSocket.localPort}",
                        "final"
                    )
                    biddingProcessorSocket.getOutputStream().write(noMoreMessages.serialize())
                    biddingProcessorSocket.close()
                    subscriptions.dispose()
                }
            )
        } catch (e: Exception) {
            println("Nu ma pot conecta la BiddingProcessor!")
            messageProcessorSocket.close()
            exitProcess(1)
        }
    }

    fun run() {
        receiveAndProcessMessages()
    }
}

fun main(args: Array<String>) {
    val messageProcessorMicroservice = MessageProcessorMicroservice()
    messageProcessorMicroservice.run()
}
