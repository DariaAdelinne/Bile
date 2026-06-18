package com.sd.laborator

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger

/** Adresa unei replici a serviciului principal (WeatherAppController). */
data class Replica(val host: String, val port: Int) {
    override fun toString() = "$host:$port"
}

/**
 * ReplicaPool - implementeaza MODELUL DE REPLICARE peste cele N replici identice ale serviciului
 * principal: alege o replica round-robin (distributie de incarcare) si, daca aceasta nu raspunde,
 * incearca urmatoarea (FAILOVER), pana epuizeaza toate replicile.
 *
 * SOLID(S): singura responsabilitate = selectia replicii + dialogul TCP cu ea (cu failover).
 */
class ReplicaPool(private val replicas: List<Replica>) {
    private val rr = AtomicInteger(0)

    init {
        println("[ReplicaPool] $N replici configurate: ${replicas.joinToString(", ")}")
    }

    val N: Int get() = replicas.size

    /** Trimite "GET <city>" unei replici (round-robin, cu failover) si intoarce raspunsul ei brut. */
    fun query(city: String): String {
        val n = replicas.size
        val start = Math.floorMod(rr.getAndIncrement(), n)
        var lastError = "nicio replica disponibila"
        for (k in 0 until n) {
            val idx = (start + k) % n
            val replica = replicas[idx]
            try {
                val resp = ask(replica, city)
                if (k > 0) println("[ReplicaPool] FAILOVER -> raspuns servit de $replica")
                return resp
            } catch (e: Exception) {
                lastError = "${replica}: ${e.message}"
                println("[ReplicaPool] Replica $replica indisponibila (${e.message}); incerc urmatoarea...")
            }
        }
        return "ERR $city|toate replicile au esuat ($lastError)"
    }

    private fun ask(replica: Replica, city: String): String {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(replica.host, replica.port), 2000)
            socket.soTimeout = 4000
            socket.getOutputStream().write("GET $city\n".toByteArray())
            socket.getOutputStream().flush()
            val reader = BufferedReader(InputStreamReader(socket.inputStream))
            return reader.readLine() ?: throw java.io.IOException("raspuns gol")
        }
    }
}
