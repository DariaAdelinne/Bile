package com.sd.laborator.interfaces

import com.sd.laborator.pojo.CacheEntry

/**
 * Interfata pentru serviciul de cache.
 * Separata de ICheltuieliService respectand ISP:
 * clientii care nu au nevoie de cache nu depind de aceasta interfata.
 *
 * Cache-ul raspunde daca o cerere a aparut in ultimele 30 de minute.
 */
interface ICacheService {
    /**
     * Verifica daca exista o intrare valida in cache pentru cheia data.
     * O intrare este valida daca a fost adaugata in ultimele 30 de minute.
     *
     * @param requestKey cheia cererii
     * @return CacheEntry daca exista si e valida, null altfel
     */
    fun get(requestKey: String): CacheEntry?

    /**
     * Adauga sau actualizeaza o intrare in cache.
     *
     * @param requestKey cheia cererii
     * @param result     rezultatul de salvat
     */
    fun put(requestKey: String, result: String)

    /**
     * Invalideaza (sterge) o intrare din cache.
     */
    fun invalidate(requestKey: String)

    /**
     * Returneaza toate intrarile curente din cache (pentru replicare).
     */
    fun getAllEntries(): Map<String, CacheEntry>

    /**
     * Inlocuieste complet continutul cache-ului (folosit de replicare
     * pentru sincronizarea cache-ului global).
     */
    fun replaceAll(entries: Map<String, CacheEntry>)
}
