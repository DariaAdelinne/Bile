package com.sd.laborator.functions

import com.sd.laborator.interfaces.IStateFunction
import com.sd.laborator.statemachine.MachineState
import com.sd.laborator.statemachine.StateContext
import org.springframework.stereotype.Component

/**
 * Functie serverless — starea ERROR.
 *
 * Gestioneaza erorile: logheaza motivul si marcheaza contextul ca terminal.
 *
 * Principiul S (SRP): singura responsabilitate este gestionarea erorilor.
 * Principiul O (OCP): se poate extinde cu notificari (email, alert) fara
 *                     a modifica celelalte functii.
 */
@Component
class ErrorStateFunction : IStateFunction {

    override fun canHandle(state: MachineState) = state == MachineState.ERROR

    override fun stateName() = MachineState.ERROR

    override fun apply(ctx: StateContext): StateContext {
        println("[ErrorStateFunction] EROARE id=${ctx.id} | Motiv: ${ctx.errorMessage}")
        // stare terminala — returnam neschimbat
        return ctx
    }
}
