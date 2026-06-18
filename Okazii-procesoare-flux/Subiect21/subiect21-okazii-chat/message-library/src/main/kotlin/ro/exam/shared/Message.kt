package ro.exam.shared

import java.nio.charset.StandardCharsets
import java.time.Instant

/** Contractul comun al mesajelor schimbate între microservicii. */
data class Message(
    val sender: String,
    val body: String,
    val timestampEpochMillis: Long = Instant.now().toEpochMilli()
) {
    fun serialize(): ByteArray =
        "$timestampEpochMillis\t${escape(sender)}\t${escape(body)}\n".toByteArray(StandardCharsets.UTF_8)

    fun uniqueKey(): String = "$timestampEpochMillis|$sender|$body"

    companion object {
        fun create(sender: String, body: String): Message = Message(sender, body)

        fun deserialize(bytes: ByteArray): Message = deserialize(bytes.toString(StandardCharsets.UTF_8))

        fun deserialize(line: String): Message {
            val fields = line.trimEnd('\r', '\n').split('\t', limit = 3)
            require(fields.size == 3) { "Mesaj invalid: sunt necesare 3 câmpuri" }
            return Message(sender = unescape(fields[1]), body = unescape(fields[2]), timestampEpochMillis = fields[0].toLong())
        }


        private fun escape(value: String): String = value
            .replace("\\", "\\\\")
            .replace("\t", "\\t")
            .replace("\n", "\\n")

        private fun unescape(value: String): String {
            val out = StringBuilder()
            var escaped = false
            for (ch in value) {
                if (escaped) {
                    out.append(when (ch) { 't' -> '\t'; 'n' -> '\n'; '\\' -> '\\'; else -> ch })
                    escaped = false
                } else if (ch == '\\') escaped = true else out.append(ch)
            }
            if (escaped) out.append('\\')
            return out.toString()
        }
    }
}
