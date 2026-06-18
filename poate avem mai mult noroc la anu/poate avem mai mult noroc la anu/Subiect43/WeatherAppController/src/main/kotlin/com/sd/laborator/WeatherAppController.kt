package com.sd.laborator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * WeatherAppController - serviciul PRINCIPAL din Laboratorul 3, incapsulat ca microserviciu TCP.
 *
 * Asculta pe un port; la cererea "GET <oras>" intoarce datele meteo (prin OpenMeteoWeatherService):
 *   raspuns OK :  "OK <oras>|<tara>|<temp>|<vant>|<descriere>|<ora>|<sursa>|<replicaId>"
 *   raspuns ERR:  "ERR <oras>|<motiv>"
 *
 * ID-ul replicii este inclus in raspuns ca sa se vada CARE replica a raspuns (pt. demonstratia de
 * replicare / distributie round-robin).
 *
 * Corutine: cate o corutina per client conectat.
 * SOLID(S): singura responsabilitate = a expune datele meteo prin TCP (logica meteo e in serviciul intern).
 */
class WeatherAppController(private val port: Int, private val replicaId: Int) {
    private val weatherService = OpenMeteoWeatherService()

    fun start() = runBlocking {
        val server = ServerSocket(port)
        println("[Replica #$replicaId] WeatherAppController pornit pe portul $port.")
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
                println("[Replica #$replicaId] GET \"$city\"")
                val r = weatherService.getWeather(city)
                val resp = if (r == null) {
                    "ERR $city|oras negasit"
                } else {
                    "OK ${r.city}|${r.country}|${r.temperature}|${r.windspeed}|" +
                        "${r.description}|${r.time}|${r.source}|$replicaId"
                }
                out.write((resp + "\n").toByteArray()); out.flush()
            }
        } catch (e: IOException) {
            println("[Replica #$replicaId] Client deconectat: ${e.message}")
        } finally {
            try { client.close() } catch (_: IOException) {}
        }
    }
}

/**
 * main: porneste [count] replici ale serviciului principal, pe porturile [basePort .. basePort+count-1].
 *
 * Argumente:  <basePort> <count>   (implicit: 1601 3)
 *
 * Fiecare replica ruleaza pe portul ei (in productie = fiecare in masina ei virtuala). Pentru demo,
 * cele N replici sunt pornite in acelasi proces, fiecare pe ServerSocket-ul ei. Poti rula la fel de
 * bine acest program de mai multe ori cu acelasi basePort si count=1 si replicaId diferit, pentru a
 * avea fiecare replica intr-un proces/VM separat.
 */
fun main(args: Array<String>) {
    val basePort = args.getOrNull(0)?.toIntOrNull() ?: 1601
    val count = args.getOrNull(1)?.toIntOrNull() ?: 3

    println("=== Pornesc $count replici WeatherAppController pe porturile $basePort..${basePort + count - 1} ===")
    val threads = (0 until count).map { i ->
        val port = basePort + i
        val replicaId = i + 1
        thread { WeatherAppController(port, replicaId).start() }
    }
    threads.forEach { it.join() }
}
