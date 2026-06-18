package com.sd.laborator.queue

import com.sd.laborator.statemachine.StateContext
import org.springframework.stereotype.Component
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ConcurrentHashMap

/**
 * Coada partajata — verificata de toate functiile serverless.
 *
 * Principiul S (SRP): gestioneaza doar stocarea si accesul la contextele din coada.
 *
 * De ce LinkedBlockingQueue: thread-safe, suporta peek() non-blocking si
 * poll() atomic — permite fiecarei functii sa verifice starea fara race condition.
 *
 * Rezultatele finale (stari terminale) sunt stocate separat pentru interogare REST.
 */
@Component
class StateQueue {

    private val queue = LinkedBlockingQueue<StateContext>()
    private val results = ConcurrentHashMap<String, StateContext>()

    fun enqueue(ctx: StateContext) {
        queue.put(ctx)
        println("[StateQueue] Enqueued id=${ctx.id} state=${ctx.currentState} (queue size=${queue.size})")
    }

    /**
     * Preia atomically un context din coada daca starea sa corespunde
     * starii pe care functia apelanta o poate gestiona.
     * Returneaza null daca nu exista niciun element potrivit.
     */
    fun pollForState(targetState: com.sd.laborator.statemachine.MachineState): StateContext? {
        // Iteram coada si preluam primul element cu starea dorita
        val iterator = queue.iterator()
        while (iterator.hasNext()) {
            val ctx = iterator.next()
            if (ctx.currentState == targetState) {
                iterator.remove()   // eliminare atomica
                return ctx
            }
        }
        return null
    }

    fun saveResult(ctx: StateContext) {
        results[ctx.id] = ctx
    }

    fun getResult(id: String): StateContext? = results[id]

    fun getAllResults(): Map<String, StateContext> = results.toMap()

    fun queueSize(): Int = queue.size
}
