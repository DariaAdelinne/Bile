package com.sd.laborator.interfaces

import com.sd.laborator.statemachine.MachineState
import com.sd.laborator.statemachine.StateContext
import java.util.function.Function

/**
 * Interfata pentru o functie serverless care gestioneaza o stare.
 *
 * Fiecare implementare:
 *   - declara ce stare poate gestiona (canHandle)
 *   - implementeaza logica de procesare si returneaza contextul actualizat
 *
 * Principiul I (ISP): interfata mica si specializata.
 * Principiul D (DIP): StateQueue depinde de aceasta abstractizare.
 * Modelul serverless: Function<StateContext, StateContext> — intrare/iesire explicita,
 * fara efecte laterale ascunse, identic cu AWS Lambda / Micronaut FunctionBean.
 */
interface IStateFunction : Function<StateContext, StateContext> {
    fun canHandle(state: MachineState): Boolean
    fun stateName(): MachineState
}
