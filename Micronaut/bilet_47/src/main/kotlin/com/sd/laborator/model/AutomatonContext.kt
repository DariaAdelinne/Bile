package com.sd.laborator.model

import java.time.LocalDateTime

/**
 * Contextul unui element din coada Kafka.
 *
 * Contine sirul de biti primit de la utilizator via REST si pozitia curenta
 * in acel sir. Fiecare functie serverless citeste bitul curent, determina
 * starea urmatoare si publica contextul actualizat pe topic-ul starii urmatoare.
 *
 * Serializat ca JSON in Kafka (prin ObjectMapper).
 */
data class AutomatonContext(
    val id: String,
    val bits: String,               // sirul complet de biti, ex: "0110"
    val bitIndex: Int = 0,          // indicele bitului curent de procesat
    val currentState: String = "S00",
    val output: Int = 0,            // 0 sau 1 (devine 1 cand se atinge S11)
    val done: Boolean = false,
    val history: List<String> = emptyList()
) {
    fun nextBit(): Char? = if (bitIndex < bits.length) bits[bitIndex] else null

    fun transition(newState: String): AutomatonContext {
        val ts = LocalDateTime.now().toString().take(19)
        val bit = nextBit()?.toString() ?: "-"
        return copy(
            currentState = newState,
            bitIndex = bitIndex + 1,
            history = history + "[$ts] $currentState --$bit--> $newState"
        )
    }

    fun finish(output: Int): AutomatonContext {
        val ts = LocalDateTime.now().toString().take(19)
        return copy(
            done = true,
            output = output,
            history = history + "[$ts] Output=$output (toate bitii procesati)"
        )
    }
}
