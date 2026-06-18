package com.sd.laborator.services

import com.sd.laborator.chain.CityFilterHandler
import com.sd.laborator.chain.DirectorHandler
import com.sd.laborator.chain.WeatherFetchHandler
import com.sd.laborator.interfaces.IApiGateway
import com.sd.laborator.interfaces.IFileWriterService
import com.sd.laborator.interfaces.ILocationSearchService
import com.sd.laborator.pojo.WeatherChainContext
import org.springframework.stereotype.Service

// API Gateway: unic punct de intrare, construieste si porneste lantul
@Service
class ApiGatewayService(
    private val locationSearchService: ILocationSearchService,
    private val fileWriterService: IFileWriterService
) : IApiGateway {

    override fun process(cityName: String): WeatherChainContext {
        // construire lant: Fetch -> Filter -> Director(FileWriter)
        val fetchHandler = WeatherFetchHandler(locationSearchService)
        val filterHandler = CityFilterHandler()
        val directorHandler = DirectorHandler(fileWriterService)

        fetchHandler
            .setNext(filterHandler)
            .setNext(directorHandler)

        return fetchHandler.handle(WeatherChainContext(cityName = cityName))
    }
}
