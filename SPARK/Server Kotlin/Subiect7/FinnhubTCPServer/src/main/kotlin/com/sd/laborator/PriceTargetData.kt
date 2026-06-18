package com.sd.laborator

import kotlinx.serialization.Serializable

@Serializable
data class PriceTargetData(
    val symbol: String,
    val targetHigh: Double,
    val targetLow: Double,
    val targetMean: Double,
    val targetMedian: Double,
    val lastUpdated: String
)
