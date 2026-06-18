package com.sd.laborator.store

import com.sd.laborator.model.NodeResponse
import jakarta.inject.Singleton
import java.util.Collections

/**
 * ResultStore - tine in memorie rezultatele produse de functia serverless, ca sa poata fi
 * vizualizate ulterior prin REST (GET /results). Procesarea fiind asincrona (prin coada),
 * userul pune cereri si vede rezultatele aici.
 *
 * SOLID(S): singura responsabilitate = pastrarea rezultatelor procesate.
 */
@Singleton
class ResultStore {
    private val results: MutableList<NodeResponse> = Collections.synchronizedList(mutableListOf())

    fun add(response: NodeResponse) {
        results.add(response)
    }

    fun all(): List<NodeResponse> = synchronized(results) { results.toList() }

    fun clear() = results.clear()
}
