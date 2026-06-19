package com.sd.laborator.rest

import com.sd.laborator.model.AutomatonContext
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * Stocheaza rezultatele finale ale automatului (contexte cu done=true).
 *
 * SRP: singura responsabilitate este persistenta in-memory a rezultatelor.
 * DIP: controllerul REST depinde de aceasta abstractizare, nu de implementare.
 */
@Component
class ResultStore {
    private val results = ConcurrentHashMap<String, AutomatonContext>()

    fun save(ctx: AutomatonContext) { results[ctx.id] = ctx }

    fun get(id: String): AutomatonContext? = results[id]

    fun getAll(): Map<String, AutomatonContext> = results.toMap()
}
