package ro.exam.shared

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import java.util.UUID

/** Mesaj privat transportat prin procesorul de flux. */
data class ChatMessage(
    val id: String,
    val roomId: String,
    val sender: String,
    val text: String,
    val timestampEpochMillis: Long
) {
    fun serializeToText(): String = listOf(
        id,
        encode(roomId),
        encode(sender),
        encode(text),
        timestampEpochMillis.toString()
    ).joinToString("|")

    companion object {
        fun create(roomId: String, sender: String, text: String): ChatMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            roomId = roomId,
            sender = sender,
            text = text,
            timestampEpochMillis = Instant.now().toEpochMilli()
        )

        fun deserialize(value: String): ChatMessage {
            val parts = value.split('|', limit = 5)
            require(parts.size == 5) { "Mesaj chat invalid" }
            return ChatMessage(
                id = parts[0],
                roomId = decode(parts[1]),
                sender = decode(parts[2]),
                text = decode(parts[3]),
                timestampEpochMillis = parts[4].toLong()
            )
        }

        private fun encode(value: String): String = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

        private fun decode(value: String): String = String(
            Base64.getUrlDecoder().decode(value),
            StandardCharsets.UTF_8
        )
    }
}
