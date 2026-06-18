package com.sd.laborator.replication

import com.sd.laborator.interfaces.ICacheService
import com.sd.laborator.interfaces.IReplicationService
import com.sd.laborator.pojo.CacheEntry
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * Serviciul de replicare.
 *
 * Mentine un registru de replici (fiecare cu propriul cache local)
 * si stie sa combine toate cache-urile intr-un cache global unificat.
 *
 * Respecta SRP: se ocupa DOAR de coordonarea replicilor.
 * Respecta DIP: depinde de interfata ICacheService, nu de implementarea concreta.
 * Respecta OCP: adaugarea de noi tipuri de replici nu necesita modificarea acestei clase.
 *
 * Strategia de merge: in caz de conflict pe aceeasi cheie, se pastreaza
 * intrarea cu timestamp-ul cel mai recent (cea mai proaspata).
 */
@Service
class ReplicationService : IReplicationService {

    // registrul de replici: replicaId -> cache-ul sau local
    private val replici = ConcurrentHashMap<String, ICacheService>()

    override fun registerReplica(replicaId: String, cache: ICacheService) {
        replici[replicaId] = cache
        println("[ReplicationService] Replica inregistrata: $replicaId (total: ${replici.size})")
    }

    /**
     * Combina cache-urile tuturor replicilor.
     * Strategie: last-write-wins pe timestamp.
     */
    override fun mergeAllCaches(): Map<String, CacheEntry> {
        val global = HashMap<String, CacheEntry>()

        for ((replicaId, cache) in replici) {
            val entries = cache.getAllEntries()
            println("[ReplicationService] Replica '$replicaId' contribuie cu ${entries.size} intrari")

            for ((key, entry) in entries) {
                val existing = global[key]
                if (existing == null || entry.timestamp.isAfter(existing.timestamp)) {
                    global[key] = entry   // pastram intrarea mai recenta
                }
            }
        }

        println("[ReplicationService] Cache global unificat: ${global.size} intrari")
        return global
    }

    /**
     * Sincronizeaza cache-ul global catre toate replicile.
     * Dupa sync, toate replicile au acelasi continut.
     */
    override fun syncGlobalCache() {
        val global = mergeAllCaches()
        for ((replicaId, cache) in replici) {
            cache.replaceAll(global)
            println("[ReplicationService] Cache sincronizat catre replica '$replicaId'")
        }
    }

    override fun getReplicaCount(): Int = replici.size
}
