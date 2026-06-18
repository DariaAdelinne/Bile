package com.sd.laborator

import org.apache.spark.SparkConf
import org.apache.spark.streaming.Durations
import org.apache.spark.streaming.api.java.JavaStreamingContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date

const val TCP_HOST = "127.0.0.1"
const val TCP_PORT = 9999
const val BATCH_SECONDS = 3L
const val MIN_SUMMARY_LENGTH = 50

fun unixToDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm")
    return sdf.format(Date(timestamp * 1000L))
}

fun main() {
    val sparkConf = SparkConf()
        .setMaster("local[2]")
        .setAppName("FinnhubNewsStreamClient")
        // reducem verbozitatea log-urilor Spark
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
                    val article = JSONObject(trimmed)

                    val source  = article.optString("source", "")
                    val summary = article.optString("summary", "")

                    // Filtru 1: sursa trebuie sa fie ne-goala
                    if (source.isEmpty()) continue

                    // Filtru 2: rezumatul trebuie sa depaseasca 500 de caractere
                    if (summary.length <= MIN_SUMMARY_LENGTH) continue

                    filtered.add(article)
                } catch (e: Exception) {
                    println("  [WARN] JSON invalid ignorat: ${e.message}")
                }
            }

            if (filtered.isNotEmpty()) {
                println("=".repeat(70))
                println("Stiri filtrate (sursa=Yahoo + rezumat > $MIN_SUMMARY_LENGTH caractere):")
                println("=".repeat(70))
                for (article in filtered) {
                    val url      = article.optString("url", "N/A")
                    val datetime = article.optLong("datetime", 0L)
                    val headline = article.optString("headline", "(fara titlu)")
                    println("  URL:   $url")
                    println("  Data:  ${unixToDate(datetime)}")
                    println("  Titlu: $headline")
                    println("-".repeat(70))
                }
                println("=".repeat(70))
            } else {
                val total = records.count { it.trim().isNotEmpty() }
                if (total > 0) {
                    println("[RDD] $total stire(i) primita(e), niciuna nu indeplineste filtrele.")
                }
            }
        }
    }

    println("Stream Spark Kotlin pornit. Astept stiri de la $TCP_HOST:$TCP_PORT ...")
    ssc.start()
    ssc.awaitTermination()
}
