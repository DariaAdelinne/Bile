import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import kotlin.system.exitProcess

class MessageProcessorMicroservice {
    private var messageProcessorSocket: ServerSocket
    private lateinit var biddingProcessorSocket: Socket
    private var auctioneerConnection: Socket
    private var receiveInQueueObservable: Observable<String>
    private val subscriptions = CompositeDisposable()
    private val messageQueue: Queue<Message> = LinkedList<Message>()

    companion object {
        const val MESSAGE_PROCESSOR_PORT = 1600
        const val BIDDING_PROCESSOR_HOST = "localhost"
        const val BIDDING_PROCESSOR_PORT = 1700
        const val ERROR_PROCESSOR_HOST = "localhost"
        const val ERROR_PROCESSOR_PORT = 1800
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
                    emitter.onError(Exception("Eroare: AuctioneerMicroservice ${auctioneerConnection.port} a fost deconectat."))
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

    private fun reportError(errorSocket: Socket, event: ErrorEvent) {
        try {
            val writer = PrintWriter(errorSocket.getOutputStream(), true)
            writer.println(String(event.serialize()).trim())
        } catch (e: Exception) {
            println("Nu am putut raporta eroarea: $e")
        }
    }

    private fun receiveAndProcessMessages() {
        val errorSocket: Socket? = try {
            Socket(ERROR_PROCESSOR_HOST, ERROR_PROCESSOR_PORT).also {
                println("Conectat la ErrorStatisticsProcessorMicroservice pe portul $ERROR_PROCESSOR_PORT")
            }
        } catch (e: Exception) {
            println("Avertisment: Nu m-am putut conecta la ErrorStatisticsProcessor.")
            null
        }

        val seenMessages = mutableSetOf<String>()

        val receiveInQueueSubscription = receiveInQueueObservable
            .filter { raw ->
                val msg = Message.deserialize(raw.toByteArray())
                val key = "${msg.sender}:${msg.body}"
                val isDuplicate = !seenMessages.add(key)
                if (isDuplicate) {
                    println("Duplicat detectat si ignorat: $msg")
                    val errorEvent = ErrorEvent(ErrorType.DUPLICATE_BID, "Oferta duplicata de la ${msg.sender}: ${msg.body}")
                    errorSocket?.let { reportError(it, errorEvent) }
                }
                !isDuplicate
            }
            .subscribeBy(
                onNext = {
                    val message = Message.deserialize(it.toByteArray())
                    println(message)
                    messageQueue.add(message)
                },
                onComplete = {
                    errorSocket?.let { sock ->
                        try {
                            val writer = PrintWriter(sock.getOutputStream(), true)
                            writer.println("DONE")
                            sock.close()
                        } catch (e: Exception) {
                            println("Eroare la inchiderea conexiunii cu ErrorStatisticsProcessor: $e")
                        }
                    }
                    val finishedMessagesMessage = Message.create(
                        "${auctioneerConnection.localAddress}:${auctioneerConnection.localPort}", "am primit tot"
                    )
                    auctioneerConnection.getOutputStream().write(finishedMessagesMessage.serialize())
                    auctioneerConnection.close()
                    sendProcessedMessages()
                },
                onError = { err ->
                    println("Eroare: $err")
                    val errorEvent = ErrorEvent(ErrorType.BIDDER_DISCONNECTED, err.message ?: "Deconectare neasteptata")
                    errorSocket?.let { sock ->
                        reportError(sock, errorEvent)
                        val writer = PrintWriter(sock.getOutputStream(), true)
                        writer.println("DONE")
                        sock.close()
                    }
                }
            )
        subscriptions.add(receiveInQueueSubscription)
    }

    private fun sendProcessedMessages() {
        try {
            biddingProcessorSocket = Socket(BIDDING_PROCESSOR_HOST, BIDDING_PROCESSOR_PORT)
            println("Trimit urmatoarele mesaje:")
            Observable.fromIterable(messageQueue).subscribeBy(
                onNext = {
                    println(it.toString())
                    biddingProcessorSocket.getOutputStream().write(it.serialize())
                },
                onComplete = {
                    val noMoreMessages = Message.create(
                        "${biddingProcessorSocket.localAddress}:${biddingProcessorSocket.localPort}", "final"
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
