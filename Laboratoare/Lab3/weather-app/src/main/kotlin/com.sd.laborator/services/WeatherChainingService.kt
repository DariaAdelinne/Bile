package com.sd.laborator.services

import com.sd.laborator.interfaces.LocationData
import org.springframework.stereotype.Service

// Interfata pentru handler
interface Handler {
    fun setNext(next: Handler): Handler
    fun handle(input: Any): Any
}

// Clasa de baza
abstract class BaseHandler : Handler {
    private var next: Handler? = null

    override fun setNext(next: Handler): Handler {
        this.next = next
        return next
    }

    protected fun next(input: Any): Any {
        return next?.handle(input) ?: input
    }
}

// 1. Blacklist
class BlacklistHandler(
    private val geoBlacklistService: GeoBlacklistService
) : BaseHandler() {

    override fun handle(input: Any): Any {
        val location = input as String

        if (geoBlacklistService.isBlocked(location)) {
            return geoBlacklistService.buildBlockedMessage(location)
        }

        return next(location)
    }
}

// 2. Location
class LocationHandler(
    private val locationSearchService: LocationSearchService
) : BaseHandler() {

    override fun handle(input: Any): Any {
        val location = input as String

        val locationData = locationSearchService.getLocation(location)
            ?: return "Location not found"

        return next(locationData)
    }
}

// 3. Forecast
class ForecastHandler(
    private val weatherForecastService: WeatherForecastService
) : BaseHandler() {

    override fun handle(input: Any): Any {
        val loc = input as LocationData

        val forecast = weatherForecastService.getForecastData(
            loc.name,
            loc.latitude,
            loc.longitude
        ) ?: return "Could not retrieve forecast"

        return forecast // ultimul din lant
    }
}

// Service care porneste lantul
@Service
class WeatherChainingService(
    private val geoBlacklistService: GeoBlacklistService,
    private val locationSearchService: LocationSearchService,
    private val weatherForecastService: WeatherForecastService
) {

    fun getForecastWithChaining(location: String): Any {

        val blacklist = BlacklistHandler(geoBlacklistService)
        val locationHandler = LocationHandler(locationSearchService)
        val forecast = ForecastHandler(weatherForecastService)

        // legarea lantului
        blacklist.setNext(locationHandler).setNext(forecast)

        return blacklist.handle(location)
    }
}