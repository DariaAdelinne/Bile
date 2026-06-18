package com.sd.laborator

import org.apache.spark.SparkConf
import org.apache.spark.streaming.Durations
import org.apache.spark.streaming.api.java.JavaStreamingContext
import org.json.JSONObject

const val TCP_HOST = "localhost"
const val TCP_PORT = 9999
const val BATCH_SECONDS = 3L
const val TARGET_EXCHANGE = "NEW YORK STOCK EXCHANGE, INC."
const val IPO_YEAR_FROM = 2015

fun main() {
    val sparkConf = SparkConf()
        .setMaster("local[2]")
        .setAppName("FinnhubProfileStreamClient")
        .set("spark.ui.showConsoleProgress", "false")

    // flux de date direct (direct stream) cu batch interval de 3 secunde
    val ssc = JavaStreamingContext(sparkConf, Durations.seconds(BATCH_SECONDS))
    ssc.sparkContext().setLogLevel("ERROR")

    // conectare la serverul TCP Python
    val lines = ssc.socketTextStream(TCP_HOST, TCP_PORT)

    lines.foreachRDD { rdd ->
        val records = rdd.collect()
        if (records.isNotEmpty()) {
            val filtered = mutableListOf<JSONObject>()

            for (line in records) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue

                try {
                    val profile = JSONObject(trimmed)

                    val exchange = profile.optString("exchange", "")
                    val ipoStr   = profile.optString("ipo", "")      // format: "YYYY-MM-DD"

                    // Filtru 1: exchange-ul trebuie sa fie NYSE
                    if (!exchange.equals(TARGET_EXCHANGE, ignoreCase = true)) continue

                    // Filtru 2: IPO din 2015 pana in prezent
                    if (ipoStr.length < 4) continue
                    val ipoYear = ipoStr.substring(0, 4).toIntOrNull() ?: continue
                    if (ipoYear < IPO_YEAR_FROM) continue

                    filtered.add(profile)
                } catch (e: Exception) {
                    println("  [WARN] JSON invalid ignorat: ${e.message}")
                }
            }

            if (filtered.isNotEmpty()) {
                println("=".repeat(70))
                println("Companii filtrate (NYSE + IPO >= $IPO_YEAR_FROM):")
                println("=".repeat(70))
                for (profile in filtered) {
                    val name      = profile.optString("name", "N/A")
                    val phone     = profile.optString("phone", "N/A")
                    val marketCap = profile.optDouble("marketCapitalization", 0.0)
                    val ipo       = profile.optString("ipo", "N/A")
                    println("  Companie:  $name")
                    println("  Telefon:   $phone")
                    println("  MarketCap: ${"%.2f".format(marketCap)} M USD")
                    println("  IPO:       $ipo")
                    println("-".repeat(70))
                }
                println("=".repeat(70))
            } else {
                val total = records.count { it.trim().isNotEmpty() }
                if (total > 0) {
                    println("[RDD] $total profil(e) primite, niciunul nu indeplineste filtrele (NYSE + IPO >= $IPO_YEAR_FROM).")
                }
            }
        }
    }

    println("Stream Spark Kotlin pornit. Astept profiluri de la $TCP_HOST:$TCP_PORT ...")
    ssc.start()
    ssc.awaitTermination()
}
