// Clasa MonitoringMicroservice primeste loguri centralizate de la microservicii.
// Afiseaza logurile si le salveaza intr-un fisier comun.
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket

class MonitoringMicroservice {
    // Portul pe care serviciul central asteapta loguri.
    companion object Constants {
        const val MONITORING_PORT = 1900
    }

    // Porneste serverul de loguri centralizate.
    fun run() {
        // Creeaza serverul pe portul 1900.
        val serverSocket = ServerSocket(MONITORING_PORT)

        println("MonitoringMicroservice pornit pe portul $MONITORING_PORT")
        LocalLogger.log("master_system.log", "MonitoringMicroservice pornit pe portul $MONITORING_PORT")

        while (true) {
            // Asteapta un log trimis de un microserviciu.
            val connection = serverSocket.accept()

            val reader = BufferedReader(
                InputStreamReader(connection.inputStream)
            )

            // Citeste logul primit prin socket.
            val receivedLog = reader.readLine()

            if (receivedLog != null) {
                println("LOG CENTRAL: $receivedLog")
                // Salveaza logul in fisierul central master_system.log.
                LocalLogger.log("master_system.log", receivedLog)
            }

            connection.close()
        }
    }
}

// Punctul de pornire al serviciului central de monitorizare.
fun main() {
    val monitoringMicroservice = MonitoringMicroservice()
    monitoringMicroservice.run()
}