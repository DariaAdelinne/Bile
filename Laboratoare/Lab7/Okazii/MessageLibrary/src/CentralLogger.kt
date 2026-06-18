// Obiectul CentralLogger trimite loguri catre MonitoringMicroservice.
// Este folosit pentru a avea un jurnal centralizat al intregului sistem.
import java.io.PrintWriter
import java.net.Socket

object CentralLogger {
    // Trimite un mesaj de log catre serviciul central de monitorizare.
    fun log(serviceName: String, message: String) {
        try {
            // Deschide conexiune catre MonitoringMicroservice pe portul 1900.
            Socket("localhost", 1900).use { socket ->
                val writer = PrintWriter(socket.getOutputStream(), true)
                // Include numele serviciului pentru a sti sursa logului.
                writer.println("[$serviceName] $message")
            }
        } catch (e: Exception) {
            LocalLogger.log("central_logger_error.log", "Nu pot trimite log central: $e")
        }
    }
}