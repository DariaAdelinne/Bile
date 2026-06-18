package com.sd.laborator.services

import org.springframework.stereotype.Service

@Service
class GeoBlacklistService {

    private val blacklistedZones = setOf(
        "russia",
        "iran",
        "north korea",
        "moscow",
        "tehran",
        "pyongyang"
    )

    fun isBlocked(location: String): Boolean {
        return blacklistedZones.contains(location.lowercase())
    }

    fun buildBlockedMessage(location: String): String {
        return "Access to weather information is not allowed for zone: $location"
    }
}