// Clasa HeartbeatMonitorMicroservice verifica daca serviciile sunt active.
// Primeste heartbeat-uri periodice si raporteaza serviciile care par cazute.
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap

class HeartbeatMonitorMicroservice {
    // Retine ultimul moment cand fiecare serviciu a trimis heartbeat.
    private val servicesLastSeen = ConcurrentHashMap<String, Long>()

    // Portul de ascultare si limita dupa care un serviciu pare cazut.
    companion object Constants {
        const val HEARTBEAT_PORT = 1800
        const val TIMEOUT_MS = 10_000L
    }

    // Porneste serverul care primeste heartbeat-uri.
    fun run() {
        // Creeaza serverul pe portul 1800.
        val serverSocket = ServerSocket(HEARTBEAT_PORT)
        // La fiecare 2 secunde iese din accept ca sa verifice serviciile.
        serverSocket.soTimeout = 2_000

        println("HeartbeatMonitorMicroservice pornit pe portul $HEARTBEAT_PORT")
        LocalLogger.log("heartbeat_monitor.log", "HeartbeatMonitor pornit pe portul $HEARTBEAT_PORT")

        while (true) {
            try {
                // Asteapta un heartbeat de la un serviciu.
                val connection = serverSocket.accept()

                val reader = BufferedReader(
                    InputStreamReader(connection.inputStream)
                )

                // Citeste numele serviciului care a trimis heartbeat-ul.
                val serviceName = reader.readLine()

                if (serviceName != null) {
                    // Actualizeaza momentul ultimului heartbeat pentru acel serviciu.
                    servicesLastSeen[serviceName] = System.currentTimeMillis()

                    println("Heartbeat primit de la: $serviceName")
                    LocalLogger.log("heartbeat_monitor.log", "Heartbeat primit de la: $serviceName")
                }

                connection.close()
            } catch (e: SocketTimeoutException) {
                checkServices()
            }
        }
    }

    // Verifica daca vreun serviciu nu a mai trimis heartbeat la timp.
    private fun checkServices() {
        val now = System.currentTimeMillis()

        servicesLastSeen.forEach { (serviceName, lastSeen) ->
            if (now - lastSeen > TIMEOUT_MS) {
                println("ATENTIE: $serviceName nu a mai trimis heartbeat de peste ${TIMEOUT_MS / 1000} secunde.")
                LocalLogger.log(
                    "heartbeat_monitor.log",
                    "ATENTIE: $serviceName pare cazut. Ultimul heartbeat a fost acum ${(now - lastSeen) / 1000} secunde."
                )
            }
        }
    }
}

// Punctul de pornire al monitorului de heartbeat.
fun main() {
    val heartbeatMonitor = HeartbeatMonitorMicroservice()
    heartbeatMonitor.run()
}