package com.sd.laborator.functions

import com.sd.laborator.interfaces.IStateFunction
import com.sd.laborator.statemachine.MachineState
import com.sd.laborator.statemachine.StateContext
import org.springframework.stereotype.Component

/**
 * Functie serverless — starea PROCESSING.
 *
 * Executa logica de procesare: transforma payload-ul
 * (uppercase + numarare cuvinte ca exemplu de procesare reala).
 * Tranzitie:
 *   - succes    → COMPLETED
 *   - exceptie → ERROR
 *
 * Principiul S (SRP): singura responsabilitate este procesarea datelor.
 */
@Component
class ProcessingStateFunction : IStateFunction {

    override fun canHandle(state: MachineState) = state == MachineState.PROCESSING

    override fun stateName() = MachineState.PROCESSING

    override fun apply(ctx: StateContext): StateContext {
        println("[ProcessingStateFunction] Procesez: '${ctx.payload.take(40)}...' (id=${ctx.id})")

        return try {
            val wordCount = ctx.payload.trim().split("\\s+".toRegex()).size
            val processedResult = "Cuvinte: $wordCount | Continut: ${ctx.payload.uppercase()}"

            ctx
                .transition(MachineState.COMPLETED, "Procesare reusita")
                .copy(result = processedResult)
        } catch (e: Exception) {
            ctx
                .transition(MachineState.ERROR, "Exceptie la procesare: ${e.message}")
                .copy(errorMessage = e.message)
        }
    }
}
