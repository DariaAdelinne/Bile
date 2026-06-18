import java.text.SimpleDateFormat
import java.util.Date

data class ErrorEvent(
    val type: ErrorType,
    val details: String,
    val timestamp: Date = Date()
) {
    override fun toString(): String {
        val fmt = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
        return "[${fmt.format(timestamp)}] ${type.description}: $details"
    }

    fun serialize(): ByteArray = "${type.name}|$details|${timestamp.time}\n".toByteArray()

    companion object {
        fun deserialize(raw: String): ErrorEvent {
            val parts = raw.split("|")
            val type = try { ErrorType.valueOf(parts[0]) } catch (e: Exception) { ErrorType.UNKNOWN }
            val details = if (parts.size > 1) parts[1] else "N/A"
            val ts = if (parts.size > 2) Date(parts[2].toLongOrNull() ?: 0L) else Date()
            return ErrorEvent(type, details, ts)
        }
    }
}
