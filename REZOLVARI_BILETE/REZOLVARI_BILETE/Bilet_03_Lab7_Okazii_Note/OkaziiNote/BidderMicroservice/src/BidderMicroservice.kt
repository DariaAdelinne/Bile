import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import java.util.Date
import kotlin.Exception
import kotlin.math.abs
import kotlin.random.Random
import kotlin.system.exitProcess

/**
 * BidderMicroservice - MODIFICAT fata de laboratorul 7.
 *
 * Modificari adaugate pentru bilet:
 *  1. Bidderul are o identitate generata aleator: (Nume Prenume, telefon, email)
 *  2. Dupa primirea rezultatului licitatiei, bidderul trimite o evaluare
 *     aleatoare (nota 1-5) catre GradingMicroservice (port 2000).
 *
 * Principii SOLID:
 *   S - bidderul are responsabilitate clara: liciteaza + evalueaza
 *   O - logica de identitate si evaluare poate fi extinsa fara a modifica bid()
 */
class BidderMicroservice {
    private var auctioneerSocket: Socket
    private var auctionResultObservable: Observable<String>
    private var myIdentity: String = "[BIDDER_NECONECTAT]"
    private lateinit var loggerSocket: Socket

    companion object {
        const val AUCTIONEER_HOST = "localhost"
        const val AUCTIONEER_PORT = 1500
        const val MAX_BID = 10_000
        const val MIN_BID = 1_000
        const val LOGGER_HOST = "localhost"
        const val LOGGER_PORT = 2000

        // Tabele pentru generarea numelor aleatoare
        private val prenume = mapOf(
            1 to "Cojocaru", 2 to "Alexandrescu", 3 to "Popescu",
            4 to "Baciu",    5 to "Grosu",        6 to "Dascalu",
            7 to "Botezatu", 8 to "Irimescu",     9 to "Cretu",
            10 to "Danciu"
        )
        private val nume = mapOf(
            1 to "Cosmin", 2 to "Radu",   3 to "Mihai",
            4 to "Ion",    5 to "Teodor", 6 to "Stefan",
            7 to "Bogdan", 8 to "Andrei", 9 to "Vlad",
            10 to "Catalin"
        )

        fun generateIdentity(): String {
            val rnd = java.util.Random(Date().time)
            val fn = rnd.nextInt(prenume.size) + 1
            val ln = rnd.nextInt(nume.size) + 1
            val name = "${prenume[fn]} ${nume[ln]}"
            val email = "${prenume[fn]!!.lowercase()}_${nume[ln]!!.lowercase()}@gmail.com"
            val phone = "+407${abs(rnd.nextInt())}"
            return "($name, $phone, $email)"
        }
    }

    init {
        try {
            // Se conecteaza mai intai la GradingMicroservice pentru a inregistra evaluarea ulterior
            loggerSocket = Socket(LOGGER_HOST, LOGGER_PORT)

            // Se genereaza identitatea aleatoare a bidderului
            myIdentity = generateIdentity()
            println("Bidder conectat cu identitatea: $myIdentity")

            // Se conecteaza la Auctioneer
            auctioneerSocket = Socket(AUCTIONEER_HOST, AUCTIONEER_PORT)
            println("$myIdentity M-am conectat la Auctioneer!")

            // Observable pentru rezultatul licitatiei
            auctionResultObservable = Observable.create<String> { emitter ->
                val bufferReader = BufferedReader(InputStreamReader(auctioneerSocket.inputStream))
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
            println("$myIdentity Nu ma pot conecta! $e")
            exitProcess(1)
        }
    }

    private fun bid() {
        val pret = Random.nextInt(MIN_BID, MAX_BID)
        val biddingMessage = Message.create(myIdentity, "liciteaza $pret")
        val serializedMessage = biddingMessage.serialize()
        auctioneerSocket.getOutputStream().write(serializedMessage)

        // Simulare eroare: sansa de a trimite oferta duplicata
        if (Random.nextBoolean()) {
            auctioneerSocket.getOutputStream().write(serializedMessage)
        }
    }

    private fun waitForResult() {
        println("$myIdentity Astept rezultatul licitatiei...")

        val auctionResultSubscription = auctionResultObservable.subscribeBy(
            onNext = {
                val resultMessage = Message.deserialize(it.toByteArray())
                println("$myIdentity Rezultat licitatie: ${resultMessage.body}")

                // === MODIFICARE FATA DE LAB ===
                // Dupa primirea rezultatului, bidderul evalueaza calitatea serviciului (nota 1-5)
                val nota = Random.nextInt(1, 6) // [1, 5]
                val evaluare = "Persoana $myIdentity a dat nota $nota"
                println("$myIdentity Trimit evaluare: $evaluare")
                loggerSocket.getOutputStream().write((evaluare + "\n").toByteArray())
                loggerSocket.getOutputStream().flush()
                loggerSocket.close()
                // ==============================
            },
            onError = {
                println("$myIdentity Eroare: $it")
            }
        )
        auctionResultSubscription.dispose()
    }

    fun run() {
        bid()
        waitForResult()
    }
}

fun main(args: Array<String>) {
    val bidderMicroservice = BidderMicroservice()
    bidderMicroservice.run()
}
