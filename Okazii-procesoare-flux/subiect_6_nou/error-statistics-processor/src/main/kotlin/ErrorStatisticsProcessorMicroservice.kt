import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket

class ErrorStatisticsProcessorMicroservice {
    private val serverSocket: ServerSocket
    private val subscriptions = CompositeDisposable()
    private val collectedErrors = mutableListOf<ErrorEvent>()
    private val classifier: IErrorClassifier = ErrorClassifier()
    private val writer: IStatisticsWriter = FileStatisticsWriter()

    companion object {
        const val ERROR_PROCESSOR_PORT = 1800
        const val OUTPUT_FILE = "auction_errors.txt"
    }

    init {
        serverSocket = ServerSocket(ERROR_PROCESSOR_PORT)
        println("ErrorStatisticsProcessorMicroservice pornit pe portul: $ERROR_PROCESSOR_PORT")
        println("Astept rapoarte de erori...")
    }

    fun run() {
        val connection = serverSocket.accept()
        println("Conexiune primita de la: ${connection.inetAddress.hostAddress}:${connection.port}")

        val reader = BufferedReader(InputStreamReader(connection.inputStream))

        val errorStream = Observable.create<String> { emitter ->
            while (true) {
                val line = reader.readLine()
                if (line == null) {
                    reader.close()
                    connection.close()
                    emitter.onComplete()
                    break
                }
                if (line == "DONE") {
                    emitter.onComplete()
                    break
                }
                emitter.onNext(line)
            }
        }

        val sub = errorStream.subscribeBy(
            onNext = { rawError ->
                val event = classifier.classify(rawError)
                collectedErrors.add(event)
                println("Eroare inregistrata: $event")
            },
            onComplete = {
                println("Licitatia s-a incheiat. Total erori: ${collectedErrors.size}")
                writer.write(collectedErrors, OUTPUT_FILE)
                serverSocket.close()
                subscriptions.dispose()
            },
            onError = { err ->
                println("Eroare conexiune: $err")
                writer.write(collectedErrors, OUTPUT_FILE)
                serverSocket.close()
            }
        )
        subscriptions.add(sub)
    }
}

fun main(args: Array<String>) {
    val processor = ErrorStatisticsProcessorMicroservice()
    processor.run()
}
