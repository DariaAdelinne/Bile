// Clasa AuctioneerMicroservice coordoneaza licitatia.
// Primeste oferte de la bidderi, le trimite la MessageProcessor,
// apoi primeste castigatorul de la BiddingProcessor si anunta participantii.
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.LinkedList
import java.util.Queue
import kotlin.system.exitProcess

class AuctioneerMicroservice {
    // Socket server pe care Auctioneer asteapta conexiuni de la bidderi.
    private var auctioneerSocket: ServerSocket
    // Conexiune catre MessageProcessor, folosita dupa ce licitatia se termina.
    private lateinit var messageProcessorSocket: Socket
    // Flux reactiv care emite ofertele primite de la bidderi.
    private var receiveBidsObservable: Observable<String>
    // Retine abonamentele RxJava ca sa poata fi inchise corect.
    private val subscriptions = CompositeDisposable()
    // Coada in care se pastreaza ofertele pana la procesare.
    private val bidQueue: Queue<Message> = LinkedList()
    // Lista conexiunilor cu bidderii, necesara pentru trimiterea rezultatului final.
    private val bidderConnections: MutableList<Socket> = mutableListOf()

    // Constante pentru porturi, host si durata licitatiei.
    companion object Constants {
        const val MESSAGE_PROCESSOR_HOST = "localhost"
        const val MESSAGE_PROCESSOR_PORT = 1600
        const val AUCTIONEER_PORT = 1500
        const val AUCTION_DURATION: Long = 15_000
    }

    // Blocul init porneste serverul si pregateste primirea ofertelor.
    init {
        // Creeaza serverul pe portul Auctioneer.
        auctioneerSocket = ServerSocket(AUCTIONEER_PORT)
        // Dupa aceasta durata, acceptarea ofertelor se opreste.
        auctioneerSocket.soTimeout = AUCTION_DURATION.toInt()

        println("AuctioneerMicroservice se executa pe portul: ${auctioneerSocket.localPort}")
        println("Se asteapta oferte de la bidderi...")
        LocalLogger.log("auctioneer.log", "AuctioneerMicroservice pornit pe portul ${auctioneerSocket.localPort}")
        CentralLogger.log("Auctioneer", "Pornit pe portul ${auctioneerSocket.localPort}")
        LocalLogger.log("auctioneer.log", "Se asteapta oferte de la bidderi")
        // Observable-ul asteapta conexiuni si transforma fiecare oferta intr-un eveniment.
        receiveBidsObservable = Observable.create<String> { emitter ->
            while (true) {
                try {
                    // Asteapta conectarea unui bidder.
                    val bidderConnection = auctioneerSocket.accept()
                    bidderConnections.add(bidderConnection)

                    val bufferReader = BufferedReader(
                        InputStreamReader(bidderConnection.inputStream)
                    )

                    // Citeste oferta trimisa de bidder.
                    val receivedMessage = bufferReader.readLine()

                    if (receivedMessage == null) {
                        bufferReader.close()
                        bidderConnection.close()
                        emitter.onError(
                            Exception("Eroare: Bidder-ul ${bidderConnection.port} a fost deconectat.")
                        )
                    } else {
                        emitter.onNext(receivedMessage)
                    }
                } catch (e: SocketTimeoutException) {
                    // Timeout-ul indica faptul ca timpul licitatiei a expirat.
                    emitter.onComplete()
                    break
                }
            }
        }
    }

    // Aboneaza codul la fluxul de oferte si salveaza mesajele primite.
    private fun receiveBids() {
        val receiveBidsSubscription = receiveBidsObservable.subscribeBy(
            onNext = {
                // Converteste mesajul serializat in obiect Message.
                val message = Message.deserialize(it.toByteArray())
                println(message)
                // Adauga oferta in coada pentru trimitere ulterioara.
                bidQueue.add(message)
                LocalLogger.log("auctioneer.log", "Oferta primita: $message")
                CentralLogger.log("Auctioneer", "Oferta primita: $message")
            },
            onComplete = {
                println("Licitatia s-a incheiat! Se trimit ofertele spre procesare...")
                LocalLogger.log("auctioneer.log", "Licitatia s-a incheiat. Se trimit ofertele spre procesare")
                forwardBids()
            },
            onError = {
                println("Eroare: $it")
            }
        )

        subscriptions.add(receiveBidsSubscription)
    }

    // Trimite toate ofertele catre MessageProcessor.
    private fun forwardBids() {
        try {
            // Deschide conexiunea catre MessageProcessor.
            messageProcessorSocket = Socket(MESSAGE_PROCESSOR_HOST, MESSAGE_PROCESSOR_PORT)

            subscriptions.add(
                Observable.fromIterable(bidQueue).subscribeBy(
                    onNext = {
                        // Serializeaza oferta si o trimite prin socket.
                        messageProcessorSocket.getOutputStream().write(it.serialize())
                        println("Am trimis mesajul: $it")
                        LocalLogger.log("auctioneer.log", "Am trimis mesajul catre MessageProcessor: $it")
                    },
                    onComplete = {
                        println("Am trimis toate ofertele catre MessageProcessor.")

                        // Mesajul cu body = "final" anunta ca nu mai urmeaza oferte.
                        val bidEndMessage = Message.create(
                            sender = "${messageProcessorSocket.localAddress}:${messageProcessorSocket.localPort}",
                            body = "final",
                            bidderName = null,
                            bidderPhone = null,
                            bidderEmail = null
                        )
                        messageProcessorSocket.getOutputStream().write(bidEndMessage.serialize())

                        val bufferReader = BufferedReader(
                            InputStreamReader(messageProcessorSocket.inputStream)
                        )
                        bufferReader.readLine()

                        messageProcessorSocket.close()
                        finishAuction()
                    }
                )
            )
        } catch (e: Exception) {
            println("Nu ma pot conecta la MessageProcessor!")
            auctioneerSocket.close()
            exitProcess(1)
        }
    }

    // Primeste rezultatul final si anunta bidderii.
    private fun finishAuction() {
        try {
            // Asteapta conexiunea de la BiddingProcessor cu rezultatul licitatiei.
            val biddingProcessorConnection = auctioneerSocket.accept()
            val bufferReader = BufferedReader(
                InputStreamReader(biddingProcessorConnection.inputStream)
            )

            val receivedMessage = bufferReader.readLine()
            val result = Message.deserialize(receivedMessage.toByteArray())
            // Extrage pretul din textul mesajului, de forma "licitez 1234".
            val winningPrice = result.body.split(" ")[1].toInt()

            println(
                "Castigatorul licitatiei este: ${result.bidderName} | " +
                        "${result.bidderPhone} | ${result.bidderEmail} | pret: $winningPrice"
            )

            LocalLogger.log(
                "auctioneer.log",
                "Castigator: ${result.bidderName}, telefon: ${result.bidderPhone}, email: ${result.bidderEmail}, pret: $winningPrice"
            )
            CentralLogger.log(
                "Auctioneer",
                "Castigator: ${result.bidderName}, telefon: ${result.bidderPhone}, email: ${result.bidderEmail}, pret: $winningPrice"
            )
            CentralLogger.log("Auctioneer", "Pornit pe portul ${auctioneerSocket.localPort}")

            val winningMessage = Message.create(
                sender = auctioneerSocket.localSocketAddress.toString(),
                body = "Licitatie castigata! Pret castigator: $winningPrice",
                bidderName = null,
                bidderPhone = null,
                bidderEmail = null
            )

            val losingMessage = Message.create(
                sender = auctioneerSocket.localSocketAddress.toString(),
                body = "Licitatie pierduta...",
                bidderName = null,
                bidderPhone = null,
                bidderEmail = null
            )

            // Trimite mesaj de castig castigatorului si mesaj de pierdere celorlalti.
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

    // Porneste primirea ofertelor.
    fun run() {
        receiveBids()
    }
}

// Punctul de pornire al aplicatiei Auctioneer.
fun main() {
    HeartbeatClient.start("Auctioneer")
    val auctioneerMicroservice = AuctioneerMicroservice()
    auctioneerMicroservice.run()
}