package com.sd.laborator.interfaces

import com.sd.laborator.pojo.ReplicationResult

interface IReplicationService {
    fun replicate(cityName: String, instances: Int): List<ReplicationResult>
}
