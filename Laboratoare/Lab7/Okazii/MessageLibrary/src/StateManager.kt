// Obiectul StateManager salveaza si incarca starea din fisiere.
// Este folosit pentru recuperare dupa o oprire neasteptata.
import java.io.File

object StateManager {
    // Salveaza o linie de date in fisierul de stare.
    fun saveState(fileName: String, data: String) {
        File(fileName).appendText(data + "\n")
    }

    // Incarca toate liniile valide din fisierul de stare.
    fun loadState(fileName: String): List<String> {
        val file = File(fileName)
        if (!file.exists()) {
            return emptyList()
        }

        return file.readLines().filter { it.isNotBlank() }
    }

    // Curata fisierul dupa ce procesarea s-a terminat cu succes.
    fun clearState(fileName: String) {
        val file = File(fileName)
        if (file.exists()) {
            file.writeText("")
        }
    }

    // Verifica daca exista o stare neterminata care trebuie recuperata.
    fun hasUnfinishedState(fileName: String): Boolean {
        val file = File(fileName)
        return file.exists() && file.readText().isNotBlank()
    }
}