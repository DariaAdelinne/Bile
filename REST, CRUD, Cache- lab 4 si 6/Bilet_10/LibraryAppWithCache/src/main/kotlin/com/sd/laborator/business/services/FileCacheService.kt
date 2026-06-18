package com.sd.laborator.business.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.sd.laborator.business.interfaces.IFileCacheService
import com.sd.laborator.business.models.CacheRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.time.LocalDateTime

// Cache bazat pe fisier disc.
// Strategia: APPEND-ONLY - nu se sterge/suprascrie niciodata fisierul.
// Validarea se face pe baza marcajului temporal (timestamp): se cauta cea mai
// recenta intrare pentru cheia data si se verifica daca e mai veche de TTL minute.
@Service
class FileCacheService(
    @Value("\${cache.file.path}") private val cacheFilePath: String,
    @Value("\${cache.ttl.minutes}") private val ttlMinutes: Long
) : IFileCacheService {

    private val objectMapper: ObjectMapper = ObjectMapper().apply {
        registerModule(JavaTimeModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    private val cacheFile: File get() = File(cacheFilePath)

    override fun get(key: String): String? {
        if (!cacheFile.exists()) return null

        // Citim toate liniile, parsam fiecare ca CacheRecord, filtram dupa cheie
        val mostRecent = cacheFile.readLines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                try { objectMapper.readValue(line, CacheRecord::class.java) } catch (e: Exception) { null }
            }
            .filter { it.key == key }
            .maxByOrNull { it.timestamp }  // cel mai recent entry pentru aceasta cheie

        if (mostRecent == null) return null

        // Verificam daca marcajul temporal e in fereastra TTL
        val expiry = mostRecent.timestamp.plusMinutes(ttlMinutes)
        return if (LocalDateTime.now().isBefore(expiry)) {
            println("[CACHE HIT] key='$key' (stocat la ${mostRecent.timestamp}, expira la $expiry)")
            mostRecent.response
        } else {
            println("[CACHE EXPIRED] key='$key' (expirat la $expiry)")
            null
        }
    }

    override fun put(key: String, response: String) {
        val record = CacheRecord(
            key = key,
            timestamp = LocalDateTime.now(),
            response = response
        )
        // Append-only: adaugam o noua linie la sfarsitul fisierului
        val line = objectMapper.writeValueAsString(record)
        cacheFile.appendText(line + "\n")
        println("[CACHE WRITE] key='$key' -> scris in '${cacheFile.absolutePath}'")
    }
}
