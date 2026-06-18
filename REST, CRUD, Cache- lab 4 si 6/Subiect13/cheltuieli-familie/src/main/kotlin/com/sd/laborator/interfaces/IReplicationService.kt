package com.sd.laborator.interfaces

import com.sd.laborator.pojo.CacheEntry

/**
 * Interfata pentru serviciul de replicare.
 * Respecta ISP - separata de interfetele de business si cache.
 *
 * Replicarea multiplica serviciile si combina cache-urile
 * intr-un cache global unificat.
 */
interface IReplicationService {
    /**
     * Inregistreaza un cache local (al unui serviciu replicat)
     * in lista de replici.
     *
     * @param replicaId identificatorul unic al replicii
     * @param cache     referinta la serviciul de cache al replicii
     */
    fun registerReplica(replicaId: String, cache: ICacheService)

    /**
     * Combina cache-urile tuturor replicilor intr-un cache global.
     * In caz de conflict (aceeasi cheie in mai multe replici),
     * se pastreaza intrarea cea mai recenta.
     *
     * @return map-ul combinat al tuturor intrarilor de cache
     */
    fun mergeAllCaches(): Map<String, CacheEntry>

    /**
     * Propaga cache-ul global catre toate replicile
     * (sincronizare bidirectionala).
     */
    fun syncGlobalCache()

    /**
     * Returneaza numarul de replici inregistrate.
     */
    fun getReplicaCount(): Int
}
