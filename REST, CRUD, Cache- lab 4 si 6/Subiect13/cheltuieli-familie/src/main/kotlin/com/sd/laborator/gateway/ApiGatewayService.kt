package com.sd.laborator.gateway

import com.fasterxml.jackson.databind.ObjectMapper
import com.sd.laborator.interfaces.ICacheService
import com.sd.laborator.interfaces.ICheltuieliService
import com.sd.laborator.interfaces.IReplicationService
import com.sd.laborator.pojo.Cheltuiala
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Serviciul de tip API Gateway (poarta API).
 *
 * Intercepteaza toate cererile REST inainte de a ajunge la serviciul
 * de business. Verifica daca cererea exista in cache (ultimele 30 min);
 * daca da, returneaza direct din cache fara a apela serviciul.
 *
 * Respecta SRP: se ocupa DOAR de rutare si orchestrare (nu de business sau cache).
 * Respecta DIP: depinde de interfetele ICheltuieliService, ICacheService,
 *               IReplicationService - nu de implementarile concrete.
 * Respecta OCP: logica de cache/replicare poate fi modificata fara a atinge
 *               gateway-ul, prin inlocuirea implementarilor interfetelor.
 *
 * Orchestrare SOLID:
 *   Request -> ApiGatewayService -> [ICacheService] -> hit? returneaza
 *                                                   -> miss? -> ICheltuieliService
 *                                                              -> pune in cache
 *                                                              -> IReplicationService.sync
 */
@Service
class ApiGatewayService {

    @Autowired
    private lateinit var cheltuieliService: ICheltuieliService

    @Autowired
    private lateinit var cacheService: ICacheService

    @Autowired
    private lateinit var replicationService: IReplicationService

    private val objectMapper = ObjectMapper()

    // La pornire, inregistram cache-ul principal ca prima replica
    @javax.annotation.PostConstruct
    fun init() {
        replicationService.registerReplica("replica-1", cacheService)
        println("[ApiGateway] Initializat cu ${replicationService.getReplicaCount()} replica(e)")
    }

    // ── READ cu cache ──────────────────────────────────────────────────────

    fun getCheltuiala(id: Int): Pair<Cheltuiala?, Boolean> {
        val key = "GET:/cheltuiala/$id"
        val cached = cacheService.get(key)

        if (cached != null) {
            println("[ApiGateway] CACHE HIT pentru $key")
            val result = objectMapper.readValue(cached.result, Cheltuiala::class.java)
            return Pair(result, true)   // true = din cache
        }

        println("[ApiGateway] CACHE MISS pentru $key")
        val cheltuiala = cheltuieliService.getCheltuiala(id)
        if (cheltuiala != null) {
            cacheService.put(key, objectMapper.writeValueAsString(cheltuiala))
        }
        return Pair(cheltuiala, false)
    }

    fun getCheltuieli(categorie: String, platitDe: String): Pair<List<Cheltuiala>, Boolean> {
        val key = "GET:/cheltuieli?categorie=$categorie&platitDe=$platitDe"
        val cached = cacheService.get(key)

        if (cached != null) {
            println("[ApiGateway] CACHE HIT pentru $key")
            val result: List<Cheltuiala> = objectMapper.readValue(
                cached.result,
                objectMapper.typeFactory.constructCollectionType(List::class.java, Cheltuiala::class.java)
            )
            return Pair(result, true)
        }

        println("[ApiGateway] CACHE MISS pentru $key")
        val lista = cheltuieliService.getCheltuieli(categorie, platitDe)
        cacheService.put(key, objectMapper.writeValueAsString(lista))
        return Pair(lista, false)
    }

    fun getTotalCategorie(categorie: String): Pair<Double, Boolean> {
        val key = "GET:/cheltuieli/total?categorie=$categorie"
        val cached = cacheService.get(key)

        if (cached != null) {
            println("[ApiGateway] CACHE HIT pentru $key")
            return Pair(cached.result.toDouble(), true)
        }

        println("[ApiGateway] CACHE MISS pentru $key")
        val total = cheltuieliService.getTotalCategorie(categorie)
        cacheService.put(key, total.toString())
        return Pair(total, false)
    }

    // ── WRITE - invalideaza cache-ul relevant si sincronizeaza replicile ──

    fun createCheltuiala(cheltuiala: Cheltuiala) {
        cheltuieliService.createCheltuiala(cheltuiala)
        // Invalidam lista generala (cache-ul listei devine invalid)
        invalidateListCache()
        replicationService.syncGlobalCache()
    }

    fun updateCheltuiala(id: Int, cheltuiala: Cheltuiala) {
        cheltuieliService.updateCheltuiala(id, cheltuiala)
        cacheService.invalidate("GET:/cheltuiala/$id")
        invalidateListCache()
        replicationService.syncGlobalCache()
    }

    fun deleteCheltuiala(id: Int) {
        cheltuieliService.deleteCheltuiala(id)
        cacheService.invalidate("GET:/cheltuiala/$id")
        invalidateListCache()
        replicationService.syncGlobalCache()
    }

    // ── Cache info (pentru endpoint de diagnosticare) ──────────────────────

    fun getCacheInfo(): Map<String, Any> {
        val entries = cacheService.getAllEntries()
        return mapOf(
            "replici" to replicationService.getReplicaCount(),
            "intrariCache" to entries.size,
            "chei" to entries.keys.toList()
        )
    }

    fun syncCache() {
        replicationService.syncGlobalCache()
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun invalidateListCache() {
        // Invalidam toate cheile de tip lista (nu stim exact parametrii)
        cacheService.getAllEntries().keys
            .filter { it.startsWith("GET:/cheltuieli") }
            .forEach { cacheService.invalidate(it) }
    }
}
