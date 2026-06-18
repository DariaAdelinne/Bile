import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class FileStatisticsWriter : IStatisticsWriter {
    override fun write(errors: List<ErrorEvent>, outputPath: String) {
        val file = File(outputPath)
        val fmt = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")

        val content = buildString {
            appendLine("=== STATISTICI ERORI LICITATIE ===")
            appendLine("Generat la: ${fmt.format(Date())}")
            appendLine("Total erori: ${errors.size}")
            appendLine()
            appendLine("--- Distributie pe tipuri ---")
            ErrorType.values().forEach { type ->
                val count = errors.count { it.type == type }
                if (count > 0) appendLine("  ${type.description}: $count")
            }
            appendLine()
            appendLine("--- Detalii erori ---")
            if (errors.isEmpty()) {
                appendLine("  Nu au aparut erori in aceasta licitatie.")
            } else {
                errors.forEachIndexed { i, err -> appendLine("  ${i + 1}. $err") }
            }
        }

        file.writeText(content)
        println("Statisticile au fost scrise in: ${file.absolutePath}")
    }
}
