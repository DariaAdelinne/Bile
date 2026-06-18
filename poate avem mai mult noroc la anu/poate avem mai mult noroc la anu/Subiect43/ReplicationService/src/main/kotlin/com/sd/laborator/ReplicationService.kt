package com.sd.laborator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket

/**
 * ReplicationService - serviciul de REPLICARE (cerinta Problemei 08).
 *
 * Expune un SINGUR punct de acces TCP (port 1500) peste cele N replici identice ale serviciului
 * principal (WeatherAppController). Pentru fiecare cerere "GET <oras>" alege o replica round-robin
 * si, daca aceasta pica, face failover catre urmatoarea (vezi [ReplicaPool]). Astfel cele N replici
 * apar clientilor ca un singur serviciu fiabil = modelul de replicare.
 *
 * Configurarea replicilor: variabila de mediu REPLICA_ENDPOINTS ("host:port,host:port,...")
 * sau implicit localhost:1601, localhost:1602, localhost:1603.
 *
 * Corutine: cate o corutina per client conectat.
 * SOLID(S): singura responsabilitate = a oferi acces replicat (cu failover) la serviciul principal.
 * SOLID(D): depinde de abstractizarea ReplicaPool, nu de o replica anume.
 */
class ReplicationService(private val pool: ReplicaPool) {

    companion object {
        const val PORT = 1500

        fun replicasFromEnv(): List<Replica> {
            val env = System.getenv("REPLICA_ENDPOINTS")
            if (!env.isNullOrBlank()) {
                return env.split(",").map { it.trim() }.filter { it.isNotEmpty() }.map {
                    val (h, p) = it.split(":")
                    Replica(h, p.toInt())
                }
            }
            // implicit: 3 replici locale
            return listOf(Replica("localhost", 1601), Replica("localhost", 1602), Replica("localhost", 1603))
        }
    }

    fun start() = runBlocking {
        val server = ServerSocket(PORT)
        println("[ReplicationService] Pornit pe portul $PORT (peste ${pool.N} replici). Astept cereri...")
        while (true) {
            val client = server.accept()
            launch(Dispatchers.IO) { handle(client) }
        }
    }

    private fun handle(client: Socket) {
        val reader = BufferedReader(InputStreamReader(client.inputStream))
        val out = client.getOutputStream()
        try {
            while (true) {
                val line = reader.readLine() ?: break
                val parts = line.split(" ", limit = 2)
                if (parts[0] != "GET" || parts.size < 2) {
                    out.write("ERR |comanda invalida\n".toByteArray()); out.flush(); continue
                }
                val city = parts[1].trim()
                val resp = pool.query(city)
                println("[ReplicationService] GET \"$city\" -> $resp")
                out.write((resp + "\n").toByteArray()); out.flush()
            }
        } catch (e: IOException) {
            println("[ReplicationService] Client deconectat: ${e.message}")
        } finally {
            try { client.close() } catch (_: IOException) {}
        }
    }
}

fun main() {
    val pool = ReplicaPool(ReplicationService.replicasFromEnv())
    ReplicationService(pool).start()
}
