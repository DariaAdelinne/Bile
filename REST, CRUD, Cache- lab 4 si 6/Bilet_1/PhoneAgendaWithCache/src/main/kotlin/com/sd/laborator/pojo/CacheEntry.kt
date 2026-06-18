package com.sd.laborator.pojo

import java.time.LocalDateTime

data class CacheEntry(
    val responseBody: Any?,
    val timestamp: LocalDateTime = LocalDateTime.now()
)
