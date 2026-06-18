package com.sd.laborator.model

import io.micronaut.core.annotation.Introspected

/**
 * Corpul cererii REST de la user: nodul de plecare + tranzitia dorita.
 * (ex: entryNode="DRAFT", transition="submit")
 */
@Introspected
data class TransitionRequest(
    val entryNode: String,
    val transition: String
)

/**
 * NodeRequest - paralela la "EratosteneRequest" din laborator. Impacheteaza datele obtinute din
 * destructurarea mesajului scos din coada (nodul curent + tranzitia ceruta).
 */
@Introspected
data class NodeRequest(
    val entryNode: String,
    val transition: String
)

/**
 * NodeResponse - paralela la "EratosteneResponse". Impacheteaza rezultatul aplicarii logicii de graf:
 * nodul destinatie (daca tranzitia e valida) sau "no se puede" (daca nu e).
 */
@Introspected
data class NodeResponse(
    val entryNode: String,
    val transition: String,
    val destination: String,
    val valid: Boolean
)
