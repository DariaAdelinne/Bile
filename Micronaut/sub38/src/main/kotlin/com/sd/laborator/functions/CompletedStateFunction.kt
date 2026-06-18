package com.sd.laborator.functions

import com.sd.laborator.interfaces.IStateFunction
import com.sd.laborator.statemachine.MachineState
import com.sd.laborator.statemachine.StateContext
import org.springframework.stereotype.Component

/**
 * Functie serverless — starea COMPLETED.
 *
 * Finalizeaza procesarea: afiseaza rezultatul si marcheaza contextul
 * ca terminal (nu mai are tranzitii ulterioare).
 *
 * Principiul S (SRP): singura responsabilitate este finalizarea.
 */
@Component
class CompletedStateFunction : IStateFunction {

    override fun canHandle(state: MachineState) = state == MachineState.COMPLETED

    override fun stateName() = MachineState.COMPLETED

    override fun apply(ctx: StateContext): StateContext {
        println("[CompletedStateFunction] SUCCES id=${ctx.id} | Rezultat: ${ctx.result}")
        // stare terminala — returnam neschimbat
        return ctx
    }
}
