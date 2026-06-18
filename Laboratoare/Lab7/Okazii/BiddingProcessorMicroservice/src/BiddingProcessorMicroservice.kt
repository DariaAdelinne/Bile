// Clasa BiddingProcessorMicroservice stabileste castigatorul licitatiei.
// Primeste ofertele sortate/procesate, alege oferta maxima si o trimite la Auctioneer.
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.util.LinkedList
import java.util.Queue
import kotlin.system.exitProcess

class BiddingProcessorMicroservice {
    // Serverul pe care BiddingProcessor asteapta mesaje de la MessageProcessor.
    private var biddingProcessorSocket: ServerSocket
    // Conexiunea catre Auctioneer, folosita pentru trimiterea castigatorului.
    private lateinit var auctioneerSocket: Socket
    // Flux reactiv pentru ofertele primite de la MessageProcessor.
    private var receiveProcessedBidsObservable: Observable<String>
    private val subscriptions = CompositeDisposable()
    // Coada cu ofertele care vor fi analizate pentru alegerea castigatorului.
    private val processedBidsQueue: Queue<Message> = LinkedList()

    // Constante pentru porturile si hostul folosite la comunicare.
    companion object Constants {
        const val BIDDING_PROCESSOR_PORT = 1700
        const val AUCTIONEER_PORT = 1500
        const val AUCTIONEER_HOST = "localhost"
    }

    // Blocul init porneste serverul si pregateste primirea ofertelor procesate.
    init {
        // Porneste serverul BiddingProcessor pe portul 1700.
        biddingProcessorSocket = ServerSocket(BIDDING_PROCESSOR_PORT)
        println("BiddingProcessorMicroservice se executa pe portul: ${biddingProcessorSocket.localPort}")
        println("Se asteapta ofertele pentru finalizarea licitatiei...")
        LocalLogger.log("bidding_processor.log", "BiddingProcessor pornit pe portul ${biddingProcessorSocket.localPort}")
        LocalLogger.log("bidding_processor.log", "Se asteapta ofertele pentru finalizarea licitatiei")
        CentralLogger.log("BiddingProcessor", "Pornit pe portul ${biddingProcessorSocket.localPort}")

        // Asteapta conectarea MessageProcessor-ului.
        val messageProcessorConnection = biddingProcessorSocket.accept()
        val bufferReader = BufferedReader(
            InputStreamReader(messageProcessorConnection.inputStream)
        )

        // Citeste mesajele pana primeste mesajul special "final".
        receiveProcessedBidsObservable = Observable.create<String> { emitter ->
            while (true) {
                val receivedMessage = bufferReader.readLine()

                if (receivedMessage == null) {
                    bufferReader.close()
                    messageProcessorConnection.close()
                    emitter.onError(
                        Exception("Eroare: MessageProcessorMicroservice ${messageProcessorConnection.port} a fost deconectat.")
                    )
                    break
                }

                // Cand primeste "final", stie ca nu mai urmeaza alte oferte.
                if (Message.deserialize(receivedMessage.toByteArray()).body == "final") {
                    emitter.onComplete()

                    val finishedBidsMessage = Message.create(
                        sender = "${messageProcessorConnection.localAddress}:${messageProcessorConnection.localPort}",
                        body = "am primit tot",
                        bidderName = null,
                        bidderPhone = null,
                        bidderEmail = null
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

    // Salveaza ofertele primite si porneste calculul castigatorului la final.
    private fun receiveProcessedBids() {
        val receiveProcessedBidsSubscription = receiveProcessedBidsObservable.subscribeBy(
            onNext = {
                // Reconstruieste obiectul Message din textul primit.
                val message = Message.deserialize(it.toByteArray())
                println(message)
                processedBidsQueue.add(message)
                LocalLogger.log("bidding_processor.log", "Oferta primita: $message")
            },
            onComplete = {
                decideAuctionWinner()
            },
            onError = {
                println("Eroare: $it")
            }
        )

        subscriptions.add(receiveProcessedBidsSubscription)
    }

    // Alege oferta cu pretul cel mai mare.
    private fun decideAuctionWinner() {
        // maxByOrNull parcurge ofertele si returneaza mesajul cu valoarea maxima.
        val winner: Message? = processedBidsQueue.toList().maxByOrNull {
            it.body.split(" ")[1].toInt()
        }

        println(
            "Castigatorul este: ${winner?.bidderName} | " +
                    "${winner?.bidderPhone} | ${winner?.bidderEmail}"
        )
        LocalLogger.log(
            "bidding_processor.log",
            "Castigator decis: ${winner?.bidderName}, telefon: ${winner?.bidderPhone}, email: ${winner?.bidderEmail}, oferta: ${winner?.body}"
        )
        CentralLogger.log(
            "BiddingProcessor",
            "Castigator decis: ${winner?.bidderName}, telefon: ${winner?.bidderPhone}, email: ${winner?.bidderEmail}, oferta: ${winner?.body}"
        )

        try {
            // Se conecteaza inapoi la Auctioneer pentru a trimite castigatorul.
            auctioneerSocket = Socket(AUCTIONEER_HOST, AUCTIONEER_PORT)
            auctioneerSocket.getOutputStream().write(winner!!.serialize())
            auctioneerSocket.close()
            println("Am anuntat castigatorul catre AuctioneerMicroservice.")
            LocalLogger.log("bidding_processor.log", "Am anuntat castigatorul catre AuctioneerMicroservice")
        } catch (e: Exception) {
            println("Nu ma pot conecta la Auctioneer!")
            biddingProcessorSocket.close()
            exitProcess(1)
        }
    }

    // Porneste primirea ofertelor procesate.
    fun run() {
        receiveProcessedBids()
        subscriptions.dispose()
    }
}

// Punctul de pornire al aplicatiei BiddingProcessor.
fun main() {
    HeartbeatClient.start("BiddingProcessor")
    val biddingProcessorMicroservice = BiddingProcessorMicroservice()
    biddingProcessorMicroservice.run()
}