package com.sd.laborator.functions

import com.sd.laborator.interfaces.IStateFunction
import com.sd.laborator.statemachine.MachineState
import com.sd.laborator.statemachine.StateContext
import org.springframework.stereotype.Component

/**
 * Functie serverless — starea VALIDATING.
 *
 * Verifica daca payload-ul este valid (nevid, lungime rezonabila).
 * Tranzitie:
 *   - valid   → PROCESSING
 *   - invalid → ERROR
 *
 * Principiul S (SRP): singura responsabilitate este validarea datelor.
 * Principiul O (OCP): logica de validare poate fi extinsa fara a modifica celelalte functii.
 */
@Component
class ValidatingStateFunction : IStateFunction {

    override fun canHandle(state: MachineState) = state == MachineState.SUBMITTED

    override fun stateName() = MachineState.SUBMITTED

    override fun apply(ctx: StateContext): StateContext {
        println("[ValidatingStateFunction] Validez: '${ctx.payload}' (id=${ctx.id})")

        if (ctx.payload.isBlank()) {
            return ctx
                .transition(MachineState.ERROR, "Payload gol")
                .copy(errorMessage = "Payload-ul nu poate fi gol")
        }

        if (ctx.payload.length > 500) {
            return ctx
                .transition(MachineState.ERROR, "Payload prea lung (>${500})")
                .copy(errorMessage = "Payload depaseste 500 caractere")
        }

        return ctx.transition(MachineState.PROCESSING, "Validare reusita")
    }
}
