package com.sd.laborator.queue

import com.sd.laborator.interfaces.IStateFunction
import com.sd.laborator.statemachine.MachineState
import com.sd.laborator.statemachine.StateContext
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

/**
 * Worker pentru o singura functie serverless.
 *
 * Ruleaza intr-un thread dedicat si verifica coada la interval regulat.
 * Cand gaseste un element in starea sa, il preia, il proceseaza si
 * pune rezultatul inapoi in coada (daca nu e stare terminala).
 *
 * Principiul S (SRP): singura responsabilitate este sa ruleze o functie
 *                     serverless in loop pe coada partajata.
 */
class StateFunctionWorker(
    private val function: IStateFunction,
    private val queue: StateQueue
) : Runnable {

    private val terminalStates = setOf(MachineState.COMPLETED, MachineState.ERROR)

    override fun run() {
        val name = function.stateName().name
        println("[StateFunctionWorker-$name] Pornit. Verific coada...")

        while (!Thread.currentThread().isInterrupted) {
            try {
                val ctx: StateContext? = queue.pollForState(function.stateName())

                if (ctx != null) {
                    println("[StateFunctionWorker-$name] Preluat id=${ctx.id}")
                    val processed = function.apply(ctx)

                    if (processed.currentState in terminalStates) {
                        // stare terminala — salvam rezultatul pentru interogare REST
                        queue.saveResult(processed)
                    } else {
                        // stare intermediara — punem inapoi in coada pentru urmatoarea functie
                        queue.enqueue(processed)
                    }
                } else {
                    // Niciun element de procesat — pauza scurta pentru a nu consuma CPU inutil
                    Thread.sleep(200)
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            } catch (e: Exception) {
                println("[StateFunctionWorker-$name] Exceptie neasteptata: ${e.message}")
            }
        }

        println("[StateFunctionWorker-$name] Oprit.")
    }
}


/**
 * Orchestratorul care porneste cate un StateFunctionWorker pentru fiecare
 * functie serverless inregistrata in contextul Spring.
 *
 * Principiul D (DIP): depinde de lista de IStateFunction (abstractizare),
 *                     nu de implementarile concrete.
 * Principiul O (OCP): adaugarea unei noi functii serverless = adaugarea unui
 *                     nou @Component IStateFunction — fara modificari aici.
 */
@Component
class ServerlessOrchestrator(
    private val functions: List<IStateFunction>,
    private val queue: StateQueue
) {

    @PostConstruct
    fun startWorkers() {
        println("[ServerlessOrchestrator] Pornesc ${functions.size} workeri serverless:")
        functions.forEach { fn ->
            val worker = StateFunctionWorker(fn, queue)
            val thread = Thread(worker, "worker-${fn.stateName().name}")
            thread.isDaemon = true
            thread.start()
            println("  -> Worker pentru starea ${fn.stateName().name} pornit")
        }
    }
}
