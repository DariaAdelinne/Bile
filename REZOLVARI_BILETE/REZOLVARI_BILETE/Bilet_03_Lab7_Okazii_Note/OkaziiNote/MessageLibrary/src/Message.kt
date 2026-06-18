import java.text.SimpleDateFormat
import java.util.*

/**
 * Clasa Message - biblioteca comuna folosita de toate microserviciile.
 * Suporta doua formate de sender:
 *  - IP:port  (ex: "localhost:4848")
 *  - (Nume Prenume, telefon, email)  (ex: "(Popescu Ion, +40712345678, pop_ion@gmail.com)")
 */
class Message private constructor(val sender: String, val body: String, val timestamp: Date) {

    companion object {
        fun create(sender: String, body: String): Message {
            return Message(sender, body, Date())
        }

        fun deserialize(msg: ByteArray): Message {
            val msgString = String(msg).trimEnd()

            // Format IP:port sau mesaj "final"
            if (msgString.contains("final") || msgString.contains(Regex("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+"))) {
                val (timestamp, sender, body) = msgString.split(' ', limit = 3)
                return Message(sender, body, Date(timestamp.toLong()))
            }

            // Format "(Nume Prenume, telefon, email) body"
            val timestamp = msgString.substringBefore(" (")
            val sender = "(" + msgString.substringAfter("(").substringBefore(")") + ")"
            val body = msgString.substringAfter(") ")
            return Message(sender, body, Date(timestamp.toLong()))
        }
    }

    fun serialize(): ByteArray {
        return "${timestamp.time} $sender $body\n".toByteArray()
    }

    override fun toString(): String {
        val dateString = SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(timestamp)
        return "[$dateString] $sender >>> $body"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Message) return false
        return sender == other.sender && body == other.body
    }

    override fun hashCode(): Int = 31 * sender.hashCode() + body.hashCode()
}
