package com.sd.laborator.business.models

import java.time.LocalDateTime

// O intrare in fisierul de cache (append-only).
// Fiecare linie din cache.log este un CacheRecord serializat ca JSON.
data class CacheRecord(
    val key: String,
    val timestamp: LocalDateTime,
    val response: String
)
