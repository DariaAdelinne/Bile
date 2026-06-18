// Obiectul LocalLogger scrie mesaje de log in fisiere locale.
// Fiecare log primeste automat data si ora.
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

object LocalLogger {
    // Scrie un mesaj intr-un fisier local.
    fun log(fileName: String, message: String) {
        // Creeaza timestamp-ul pentru log.
        val timestamp = SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(Date())
        // Adauga mesajul la finalul fisierului.
        File(fileName).appendText("[$timestamp] $message\n")
    }
}