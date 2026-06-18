package com.sd.laborator.interfaces

import com.sd.laborator.pojo.CacheEntry

interface ICacheService {
    fun get(key: String): CacheEntry?
    fun put(key: String, value: Any?)
    fun isValid(entry: CacheEntry): Boolean
}
