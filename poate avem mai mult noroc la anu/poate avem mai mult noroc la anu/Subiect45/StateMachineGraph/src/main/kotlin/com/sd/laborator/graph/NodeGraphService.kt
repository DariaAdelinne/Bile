package com.sd.laborator.graph

import com.sd.laborator.model.NodeRequest
import com.sd.laborator.model.NodeResponse
import jakarta.inject.Singleton

/**
 * NodeGraphService - paralela la "ErastoteneService" din laborator: descrie GRAFUL automatului.
 *
 * Graful = HashMap<String, List<Pair<tranzitie, destinatie>>>, adica:
 *     <nod_curent, [ (tranzitie, nod_destinatie), ... ]>
 *
 * Exemplu (workflow de document):
 *     DRAFT     --submit-->  REVIEW
 *     REVIEW    --approve--> PUBLISHED
 *     REVIEW    --reject-->  DRAFT
 *     PUBLISHED --archive--> ARCHIVED
 *
 * Logica (apply): daca entryNode e cheie in graf SI are tranzitia ceruta -> intoarce nodul
 * destinatie (operatie valida); altfel intoarce "no se puede" (operatie invalida).
 *
 * SOLID(S): singura responsabilitate = logica grafului (independenta de coada/REST).
 */
@Singleton
class NodeGraphService {

    private val graph: Map<String, List<Pair<String, String>>> = mapOf(
        "DRAFT" to listOf("submit" to "REVIEW"),
        "REVIEW" to listOf("approve" to "PUBLISHED", "reject" to "DRAFT"),
        "PUBLISHED" to listOf("archive" to "ARCHIVED"),
        "ARCHIVED" to emptyList()
    )

    /** Aplica tranzitia: paralela la handler.apply(EratosteneRequest) -> EratosteneResponse. */
    fun apply(request: NodeRequest): NodeResponse {
        val transitions = graph[request.entryNode]
        val destination = transitions?.firstOrNull { it.first == request.transition }?.second
        return if (destination != null) {
            NodeResponse(request.entryNode, request.transition, destination, valid = true)
        } else {
            NodeResponse(request.entryNode, request.transition, "no se puede", valid = false)
        }
    }

    /** Reprezentarea grafului (pentru endpoint-ul de vizualizare). */
    fun describe(): Map<String, List<String>> =
        graph.mapValues { entry -> entry.value.map { "${it.first} -> ${it.second}" } }
}
