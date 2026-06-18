package com.sd.laborator.cache

import com.sd.laborator.interfaces.ICacheService
import com.sd.laborator.pojo.CacheEntry
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementarea serviciului de cache cu TTL (Time-To-Live) de 30 de minute.
 *
 * Respecta SRP: se ocupa DOAR de logica de cache, nu de business.
 * Respecta OCP: TTL-ul poate fi parametrizat fara a modifica interfata.
 * Respecta LSP: poate fi inlocuit cu orice alta implementare a ICacheService
 *               (ex: RedisCache, EhCache etc.) fara a afecta clientii.
 *
 * Un request este considerat "in cache" daca a aparut in ultimele 30 de minute.
 */
@Service
class CacheService(
    private val ttlMinute: Long = 30L
) : ICacheService {

    // ConcurrentHashMap pentru thread-safety
    private val store = ConcurrentHashMap<String, CacheEntry>()

    /**
     * Verifica daca exista o intrare valida (ne-expirata) pentru cheia data.
     * O intrare este valida daca timestamp-ul sau e in ultimele [ttlMinute] minute.
     */
    override fun get(requestKey: String): CacheEntry? {
        val entry = store[requestKey] ?: return null
        val expiry = entry.timestamp.plusMinutes(ttlMinute)
        return if (LocalDateTime.now().isBefore(expiry)) {
            entry   // intrarea este inca valida
        } else {
            store.remove(requestKey)   // expirata - curatam
            null
        }
    }

    override fun put(requestKey: String, result: String) {
        store[requestKey] = CacheEntry(
            requestKey = requestKey,
            result = result,
            timestamp = LocalDateTime.now()
        )
    }

    override fun invalidate(requestKey: String) {
        store.remove(requestKey)
    }

    override fun getAllEntries(): Map<String, CacheEntry> = HashMap(store)

    override fun replaceAll(entries: Map<String, CacheEntry>) {
        store.clear()
        store.putAll(entries)
    }
}
