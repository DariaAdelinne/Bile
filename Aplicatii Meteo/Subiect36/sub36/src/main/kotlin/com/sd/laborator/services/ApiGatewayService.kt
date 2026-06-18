package com.sd.laborator.services

import com.sd.laborator.chain.LocationResolverHandler
import com.sd.laborator.chain.WeatherFetchHandler
import com.sd.laborator.chain.WeatherFilterHandler
import com.sd.laborator.chain.WeatherVisualizationHandler
import com.sd.laborator.interfaces.IApiGateway
import com.sd.laborator.interfaces.ILocationSearchService
import com.sd.laborator.pojo.WeatherChainContext
import org.springframework.stereotype.Service

// API Gateway: unic punct de intrare, construieste si porneste lantul
@Service
class ApiGatewayService(
    private val locationSearchService: ILocationSearchService
) : IApiGateway {

    override fun process(cityName: String): WeatherChainContext {
        // construire lant: LocationResolver -> WeatherFetch -> Filter -> Visualization
        val locationHandler = LocationResolverHandler(locationSearchService)
        val fetchHandler = WeatherFetchHandler()
        val filterHandler = WeatherFilterHandler()
        val visualizationHandler = WeatherVisualizationHandler()

        locationHandler
            .setNext(fetchHandler)
            .setNext(filterHandler)
            .setNext(visualizationHandler)

        val context = WeatherChainContext(cityName = cityName)
        return locationHandler.handle(context)
    }
}
