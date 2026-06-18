// Obiectul HeartbeatClient trimite periodic semnale catre HeartbeatMonitor.
// Aceste semnale arata ca microserviciul inca ruleaza.
import java.io.PrintWriter
import java.net.Socket
import kotlin.concurrent.thread

object HeartbeatClient {
    // Porneste un thread separat care trimite heartbeat-uri continuu.
    fun start(serviceName: String) {
        // Thread daemon: ruleaza in fundal si nu blocheaza inchiderea aplicatiei.
        thread(start = true, isDaemon = true) {
            while (true) {
                try {
                    // Deschide o conexiune scurta catre monitorul de heartbeat.
                    Socket("localhost", 1800).use { socket ->
                        val writer = PrintWriter(socket.getOutputStream(), true)
                        writer.println(serviceName)
                    }

                    LocalLogger.log("${serviceName.lowercase()}_heartbeat.log", "Heartbeat trimis")
                } catch (e: Exception) {
                    LocalLogger.log("${serviceName.lowercase()}_heartbeat.log", "Nu pot trimite heartbeat: $e")
                }

                // Asteapta 3 secunde pana la urmatorul heartbeat.
                Thread.sleep(3_000)
            }
        }
    }
}