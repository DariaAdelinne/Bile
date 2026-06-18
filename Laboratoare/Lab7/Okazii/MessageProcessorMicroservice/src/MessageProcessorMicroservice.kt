// Clasa MessageProcessorMicroservice proceseaza ofertele primite de la Auctioneer.
// Elimina duplicatele, salveaza starea pentru recovery, sorteaza dupa timestamp
// si trimite mesajele mai departe catre BiddingProcessor.
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

class MessageProcessorMicroservice {
    // Serverul pe care MessageProcessor primeste oferte de la Auctioneer.
    private var messageProcessorSocket: ServerSocket
    // Conexiune catre BiddingProcessor, folosita dupa procesarea mesajelor.
    private lateinit var biddingProcessorSocket: Socket
    // Conexiunea primita de la Auctioneer.
    private lateinit var auctioneerConnection: Socket
    // Flux reactiv pentru mesajele primite in coada.
    private lateinit var receiveInQueueObservable: Observable<String>

    // Retine abonamentele RxJava pentru inchidere controlata.
    private val subscriptions = CompositeDisposable()
    // Coada in care se pastreaza mesajele pana la sortare si trimitere.
    private val messageQueue: Queue<Message> = LinkedList()
    // Indica daca serviciul reporneste dintr-o stare salvata anterior.
    private var recoveryMode = false
    // Contor folosit pentru simularea unui crash dupa un numar de mesaje.
    private var receivedCount = 0
    // Constante pentru porturi, fisierul de stare si simularea de crash.
    companion object Constants {
        const val MESSAGE_PROCESSOR_PORT = 1600
        const val BIDDING_PROCESSOR_HOST = "localhost"
        const val BIDDING_PROCESSOR_PORT = 1700
        const val STATE_FILE = "message_processor_state.log"
        const val SIMULATE_CRASH = false
        const val CRASH_AFTER_MESSAGES = 5
    }

    // Blocul init porneste serverul si verifica daca exista stare de recuperat.
    init {
        // Porneste serverul MessageProcessor pe portul 1600.
        messageProcessorSocket = ServerSocket(MESSAGE_PROCESSOR_PORT)

        println("MessageProcessorMicroservice se executa pe portul: ${messageProcessorSocket.localPort}")
        println("Se asteapta mesaje pentru procesare...")

        LocalLogger.log("message_processor.log", "MessageProcessor pornit pe portul ${messageProcessorSocket.localPort}")
        CentralLogger.log("MessageProcessor", "Pornit pe portul ${messageProcessorSocket.localPort}")
        // Daca exista mesaje salvate neterminate, intra in modul recovery.
        if (StateManager.hasUnfinishedState(STATE_FILE)) {
            recoveryMode = true
            println("Recuperare stare anterioara...")

            // Incarca mesajele salvate in fisierul de stare.
            val savedMessages = StateManager.loadState(STATE_FILE)

            savedMessages.forEach {
                val msg = Message.deserialize(it.toByteArray())
                messageQueue.add(msg)
            }

            println("S-au incarcat ${messageQueue.size} mesaje din jurnal.")
            LocalLogger.log("message_processor.log", "Recovery: s-au incarcat ${messageQueue.size} mesaje din jurnal")
        } else {
            // Daca nu exista recovery, asteapta conectarea Auctioneer-ului.
            auctioneerConnection = messageProcessorSocket.accept()

            val bufferReader = BufferedReader(
                InputStreamReader(auctioneerConnection.inputStream)
            )

            // Citeste mesaje pana cand primeste mesajul special "final".
            receiveInQueueObservable = Observable.create<String> { emitter ->
                while (true) {
                    val receivedMessage = bufferReader.readLine()

                    if (receivedMessage == null) {
                        bufferReader.close()
                        auctioneerConnection.close()
                        emitter.onError(
                            Exception("Eroare: AuctioneerMicroservice ${auctioneerConnection.port} a fost deconectat.")
                        )
                        break
                    }

                    // "final" marcheaza sfarsitul listei de oferte.
                    if (Message.deserialize(receivedMessage.toByteArray()).body == "final") {
                        emitter.onComplete()
                        break
                    } else {
                        emitter.onNext(receivedMessage)
                    }
                }
            }
        }
    }

    // Primeste mesaje, elimina duplicatele si salveaza progresul pe disc.
    private fun receiveAndProcessMessages() {
        // Set folosit ca sa nu proceseze acelasi mesaj de doua ori.
        val seenMessages = mutableSetOf<String>()

        val receiveInQueueSubscription = receiveInQueueObservable
            // Lasa sa treaca doar mesajele care nu au mai fost vazute.
            .filter { raw -> seenMessages.add(raw) }
            .subscribeBy(
                onNext = {
                    val message = Message.deserialize(it.toByteArray())
                    println(message)

                    messageQueue.add(message)
                    // Salveaza mesajul in jurnal pentru recuperare in caz de crash.
                    StateManager.saveState(STATE_FILE, it)
                    receivedCount++

                    // Daca este activata simularea, opreste procesul dupa un numar de mesaje.
                    if (SIMULATE_CRASH && receivedCount == CRASH_AFTER_MESSAGES) {
                        println("CRASH SIMULAT dupa $receivedCount mesaje. Reporneste MessageProcessor.")
                        LocalLogger.log("message_processor.log", "CRASH SIMULAT dupa $receivedCount mesaje")
                        exitProcess(99)
                    }

                    LocalLogger.log("message_processor.log", "Mesaj primit si salvat in stare: $message")
                    CentralLogger.log("MessageProcessor", "Mesaj primit: $message")
                },
                onComplete = {
                    // La final, sorteaza si trimite mesajele mai departe.
                    processAndSendMessages()

                    val finishedMessagesMessage = Message.create(
                        sender = "${auctioneerConnection.localAddress}:${auctioneerConnection.localPort}",
                        body = "am primit tot",
                        bidderName = null,
                        bidderPhone = null,
                        bidderEmail = null
                    )

                    auctioneerConnection.getOutputStream().write(finishedMessagesMessage.serialize())
                    auctioneerConnection.close()
                },
                onError = {
                    println("Eroare: $it")
                    LocalLogger.log("message_processor.log", "Eroare: $it")
                }
            )

        subscriptions.add(receiveInQueueSubscription)
    }

    // Sorteaza mesajele dupa timestamp si declanseaza trimiterea lor.
    private fun processAndSendMessages() {
        // Sortarea pastreaza ordinea cronologica a ofertelor.
        val sortedMessages = messageQueue.toList().sortedBy { it.timestamp }
        messageQueue.clear()
        messageQueue.addAll(sortedMessages)

        LocalLogger.log("message_processor.log", "Mesajele au fost sortate dupa timestamp")
        CentralLogger.log("MessageProcessor", "Mesajele au fost sortate si trimise catre BiddingProcessor")
        sendProcessedMessages()

        // Dupa procesare completa, sterge starea salvata.
        StateManager.clearState(STATE_FILE)
        LocalLogger.log("message_processor.log", "Starea a fost curatata dupa procesare completa")
    }

    // Trimite mesajele procesate catre BiddingProcessor.
    private fun sendProcessedMessages() {
        try {
            // Deschide conexiunea catre BiddingProcessor.
            biddingProcessorSocket = Socket(BIDDING_PROCESSOR_HOST, BIDDING_PROCESSOR_PORT)

            println("Trimit urmatoarele mesaje:")

            Observable.fromIterable(messageQueue).subscribeBy(
                onNext = {
                    println(it.toString())
                    LocalLogger.log("message_processor.log", "Mesaj trimis catre BiddingProcessor: $it")
                    // Trimite mesajul serializat prin socket.
                    biddingProcessorSocket.getOutputStream().write(it.serialize())
                },
                onComplete = {
                    // Trimite mesajul "final" ca sa anunte sfarsitul transmiterii.
                    val noMoreMessages = Message.create(
                        sender = "${biddingProcessorSocket.localAddress}:${biddingProcessorSocket.localPort}",
                        body = "final",
                        bidderName = null,
                        bidderPhone = null,
                        bidderEmail = null
                    )

                    biddingProcessorSocket.getOutputStream().write(noMoreMessages.serialize())
                    biddingProcessorSocket.close()
                    subscriptions.dispose()
                }
            )
        } catch (e: Exception) {
            println("Nu ma pot conecta la BiddingProcessor!")
            LocalLogger.log("message_processor.log", "Nu ma pot conecta la BiddingProcessor: $e")
            messageProcessorSocket.close()
            exitProcess(1)
        }
    }

    // Decide daca ruleaza din recovery sau asteapta mesaje noi.
    fun run() {
        if (recoveryMode) {
            processAndSendMessages()
        } else {
            receiveAndProcessMessages()
        }
    }
}

// Punctul de pornire al aplicatiei MessageProcessor.
fun main() {
    HeartbeatClient.start("MessageProcessor")
    val messageProcessorMicroservice = MessageProcessorMicroservice()
    messageProcessorMicroservice.run()
}