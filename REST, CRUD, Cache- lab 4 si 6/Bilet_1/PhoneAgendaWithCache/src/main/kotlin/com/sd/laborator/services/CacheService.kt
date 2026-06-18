package com.sd.laborator.services

import com.sd.laborator.interfaces.ICacheService
import com.sd.laborator.pojo.CacheEntry
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Service
class CacheService : ICacheService {

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    override fun get(key: String): CacheEntry? = cache[key]

    override fun put(key: String, value: Any?) {
        cache[key] = CacheEntry(responseBody = value)
    }

    // Returneaza true daca cererea a fost facuta in ultimele 30 de minute
    override fun isValid(entry: CacheEntry): Boolean {
        return LocalDateTime.now().isBefore(entry.timestamp.plusMinutes(30))
    }
}
