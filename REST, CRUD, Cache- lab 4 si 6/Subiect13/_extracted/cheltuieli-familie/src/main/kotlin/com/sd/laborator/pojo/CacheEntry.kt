package com.sd.laborator.pojo

import java.time.LocalDateTime

/**
 * O intrare in cache-ul de cereri.
 * Retine rezultatul unui request si momentul in care a fost facut.
 *
 * @param requestKey  cheia unica a cererii (ex: "GET:/cheltuiala/5")
 * @param result      rezultatul serializat ca String (JSON)
 * @param timestamp   momentul inregistrarii in cache
 */
data class CacheEntry(
    val requestKey: String,
    val result: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
)
