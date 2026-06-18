package com.sd.laborator

import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * MessageRepository - stratul de acces la baza de date SQLite (filtered_messages.db).
 *
 * Tabela:
 *   messages(id INTEGER PRIMARY KEY AUTOINCREMENT, msg_type TEXT, source_port INTEGER,
 *            content TEXT, ts TEXT)
 *
 * SOLID(S): singura responsabilitate = persistenta mesajelor filtrate in SQLite
 *           (separat de logica de retea din DatabaseServiceMicroservice).
 */
class MessageRepository(dbUrl: String) {
    private val connection: Connection
    private val tsFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    init {
        // inregistreaza explicit driverul (sigur si in fat-jar)
        Class.forName("org.sqlite.JDBC")
        connection = DriverManager.getConnection(dbUrl)
        connection.createStatement().use { st ->
            st.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS messages (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    msg_type    TEXT    NOT NULL,
                    source_port INTEGER NOT NULL,
                    content     TEXT    NOT NULL,
                    ts          TEXT    NOT NULL
                )
                """.trimIndent()
            )
        }
        println("[Repository] Baza de date pregatita: $dbUrl (tabela 'messages').")
    }

    /** Insereaza un mesaj filtrat si returneaza numarul total de randuri din tabela. */
    @Synchronized
    fun insert(type: String, sourcePort: Int, content: String): Int {
        connection.prepareStatement(
            "INSERT INTO messages(msg_type, source_port, content, ts) VALUES (?, ?, ?, ?)"
        ).use { ps ->
            ps.setString(1, type)
            ps.setInt(2, sourcePort)
            ps.setString(3, content)
            ps.setString(4, LocalDateTime.now().format(tsFormat))
            ps.executeUpdate()
        }
        return count()
    }

    @Synchronized
    fun count(): Int {
        connection.createStatement().use { st ->
            st.executeQuery("SELECT COUNT(*) FROM messages").use { rs ->
                return if (rs.next()) rs.getInt(1) else 0
            }
        }
    }
}
