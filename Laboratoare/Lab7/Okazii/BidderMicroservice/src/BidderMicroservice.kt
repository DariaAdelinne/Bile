// Clasa BidderMicroservice reprezinta un participant la licitatie.
// Se conecteaza la Auctioneer, trimite o oferta si asteapta rezultatul.
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import kotlin.random.Random
import kotlin.system.exitProcess

class BidderMicroservice {
    // Socket-ul prin care bidderul comunica cu Auctioneer.
    private var auctioneerSocket: Socket
    // Flux reactiv care asteapta rezultatul final al licitatiei.
    private var auctionResultObservable: Observable<String>
    // Identitate folosita in afisari si loguri.
    private var myIdentity: String = "[BIDDER_NECONECTAT]"

    // Datele participantului la licitatie.
    private val bidderName: String
    private val bidderPhone: String
    private val bidderEmail: String

    // Constante pentru conexiune si intervalul ofertelor.
    companion object Constants {
        const val AUCTIONEER_HOST = "localhost"
        const val AUCTIONEER_PORT = 1500
        const val MAX_BID = 10_000
        const val MIN_BID = 1_000

        private val sampleNames = listOf(
            "Ion Popescu",
            "Maria Ionescu",
            "Andrei Georgescu",
            "Elena Dumitrescu",
            "Mihai Stan",
            "Ana Radu",
            "Cristian Pavel",
            "Ioana Matei"
        )
    }

    // Blocul init genereaza datele bidderului si il conecteaza la Auctioneer.
    init {
        // Alege aleator un nume pentru bidder.
        bidderName = sampleNames.random()
        bidderPhone = "07" + Random.nextInt(10000000, 99999999)
        bidderEmail = bidderName
            .lowercase()
            .replace(" ", ".") + "@email.com"

        try {
            // Se conecteaza la serverul Auctioneer.
            auctioneerSocket = Socket(AUCTIONEER_HOST, AUCTIONEER_PORT)
            println("M-am conectat la Auctioneer!")
            LocalLogger.log("bidder.log", "M-am conectat la Auctioneer")
            CentralLogger.log("Bidder", "$myIdentity conectat la Auctioneer")

            myIdentity = "[${auctioneerSocket.localPort}]"

            // Pregateste asteptarea rezultatului trimis de Auctioneer.
            auctionResultObservable = Observable.create<String> { emitter ->
                val bufferReader = BufferedReader(
                    InputStreamReader(auctioneerSocket.inputStream)
                )

                // Citeste rezultatul licitatiei.
                val receivedMessage = bufferReader.readLine()

                if (receivedMessage == null) {
                    bufferReader.close()
                    auctioneerSocket.close()
                    emitter.onError(Exception("AuctioneerMicroservice s-a deconectat."))
                    return@create
                }

                emitter.onNext(receivedMessage)
                emitter.onComplete()

                bufferReader.close()
                auctioneerSocket.close()
            }
        } catch (e: Exception) {
            println("$myIdentity Nu ma pot conecta la Auctioneer!")
            exitProcess(1)
        }
    }

    // Genereaza si trimite oferta catre Auctioneer.
    private fun bid() {
        // Pretul este generat aleator intre MIN_BID si MAX_BID.
        val pret = Random.nextInt(MIN_BID, MAX_BID)

        // Creeaza mesajul de licitare cu datele bidderului.
        val biddingMessage = Message.create(
            sender = "${auctioneerSocket.localAddress}:${auctioneerSocket.localPort}",
            body = "licitez $pret",
            bidderName = bidderName,
            bidderPhone = bidderPhone,
            bidderEmail = bidderEmail
        )

        // Serializeaza mesajul ca sa poata fi transmis prin socket.
        val serializedMessage = biddingMessage.serialize()
        auctioneerSocket.getOutputStream().write(serializedMessage)

        // Simuleaza uneori trimiterea unui duplicat, pentru testarea filtrarii duplicatelor.
        if (Random.nextBoolean()) {
            auctioneerSocket.getOutputStream().write(serializedMessage)
        }

        println("$myIdentity Am trimis oferta: $pret")
        LocalLogger.log("bidder.log", "$myIdentity Am trimis oferta: $pret")
        CentralLogger.log("Bidder", "$myIdentity oferta trimisa: $pret de catre $bidderName")
        println("$myIdentity Date bidder: $bidderName | $bidderPhone | $bidderEmail")
    }

    // Asteapta raspunsul final: castig sau pierdere.
    private fun waitForResult() {
        println("$myIdentity Astept rezultatul licitatiei...")

        val auctionResultSubscription = auctionResultObservable.subscribeBy(
            onNext = {
                // Converteste raspunsul primit in obiect Message.
                val resultMessage = Message.deserialize(it.toByteArray())
                println("$myIdentity Rezultat licitatie: ${resultMessage.body}")
                LocalLogger.log("bidder.log", "$myIdentity Rezultat licitatie: ${resultMessage.body}")
                CentralLogger.log("Bidder", "$myIdentity rezultat primit: ${resultMessage.body}")
            },
            onError = {
                println("$myIdentity Eroare: $it")
            }
        )

        auctionResultSubscription.dispose()
    }

    // Ruleaza bidderul: trimite oferta si asteapta rezultatul.
    fun run() {
        bid()
        waitForResult()
    }
}

// Punctul de pornire al aplicatiei Bidder.
fun main() {
    HeartbeatClient.start("Bidder")
    val bidderMicroservice = BidderMicroservice()
    bidderMicroservice.run()
}