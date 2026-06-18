package com.sd.laborator.business.interfaces

// Cache bazat pe fisier disc (append-only, analiza pe baza de marcaje temporale).
interface IFileCacheService {
    // Returneaza raspunsul din cache daca exista o intrare valida (< TTL minute) pentru cheia data, altfel null.
    fun get(key: String): String?

    // Adauga o noua intrare in fisierul de cache cu timestamp-ul curent (append-only).
    fun put(key: String, response: String)
}
