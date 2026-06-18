package com.sd.laborator.statemachine

import java.time.LocalDateTime

/**
 * Contextul unui element din coada — contine datele utilizatorului,
 * starea curenta si istoricul tranzitiilor.
 */
data class StateContext(
    val id: String,
    val payload: String,               // datele trimise de utilizator prin REST
    val currentState: MachineState = MachineState.SUBMITTED,
    val result: String? = null,        // rezultatul procesarii
    val errorMessage: String? = null,
    val history: List<String> = emptyList()
) {
    fun transition(newState: MachineState, note: String): StateContext {
        val ts = LocalDateTime.now().toString().take(19)
        return copy(
            currentState = newState,
            history = history + "[$ts] ${currentState.name} -> ${newState.name}: $note"
        )
    }
}
