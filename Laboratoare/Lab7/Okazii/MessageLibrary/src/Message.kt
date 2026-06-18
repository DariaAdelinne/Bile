// Clasa Message defineste formatul mesajelor trimise intre microservicii.
// Contine expeditorul, corpul mesajului, timestamp-ul si datele bidderului.
import java.text.SimpleDateFormat
import java.util.*

class Message private constructor(
    // Adresa/identitatea serviciului care a trimis mesajul.
    val sender: String,
    // Continutul mesajului, de exemplu "licitez 3000" sau "final".
    val body: String,
    // Momentul in care a fost creat mesajul.
    val timestamp: Date,
    // Date optionale despre bidder; pot fi null pentru mesaje tehnice.
    val bidderName: String?,
    val bidderPhone: String?,
    val bidderEmail: String?
) {
    companion object {
        // Creeaza un mesaj nou cu timestamp-ul curent.
        fun create(
            sender: String,
            body: String,
            bidderName: String?,
            bidderPhone: String?,
            bidderEmail: String?
        ): Message {
            return Message(sender, body, Date(), bidderName, bidderPhone, bidderEmail)
        }

        // Reconstruieste un Message din forma serializata primita prin socket.
        fun deserialize(msg: ByteArray): Message {
            val msgString = String(msg).trim()
            // Campurile sunt separate prin caracterul "|".
            val parts = msgString.split('|')

            require(parts.size >= 6) { "Mesaj invalid: $msgString" }

            return Message(
                sender = parts[1],
                body = parts[2],
                timestamp = Date(parts[0].toLong()),
                bidderName = parts[3].ifEmpty { null },
                bidderPhone = parts[4].ifEmpty { null },
                bidderEmail = parts[5].ifEmpty { null }
            )
        }
    }

    // Transforma mesajul in ByteArray ca sa poata fi transmis prin socket.
    fun serialize(): ByteArray {
        return "${timestamp.time}|$sender|$body|${bidderName ?: ""}|${bidderPhone ?: ""}|${bidderEmail ?: ""}\n".toByteArray()
    }

    // Formateaza mesajul pentru afisare clara in consola/loguri.
    override fun toString(): String {
        val dateString = SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(timestamp)
        return "[$dateString] $sender [$bidderName, $bidderPhone, $bidderEmail] >>> $body"
    }
}

// Test simplu pentru serializare si deserializare.
fun main() {
    val msg = Message.create(
        sender = "localhost:4848",
        body = "test mesaj",
        bidderName = "Ion Popescu",
        bidderPhone = "0712345678",
        bidderEmail = "ion.popescu@email.com"
    )

    println(msg)

    val serialized = msg.serialize()
    val deserialized = Message.deserialize(serialized)

    println(deserialized)
}