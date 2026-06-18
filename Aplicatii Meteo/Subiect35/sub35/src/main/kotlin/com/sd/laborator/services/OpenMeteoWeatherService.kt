package com.sd.laborator.services

import com.sd.laborator.interfaces.ILocationSearchService
import com.sd.laborator.interfaces.IWeatherService
import com.sd.laborator.pojo.WeatherPoint
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class OpenMeteoWeatherService(
    private val locationSearchService: ILocationSearchService
) : IWeatherService {

    override fun getWeather(cityName: String): List<WeatherPoint> {
        val location = locationSearchService.getLocation(cityName)
            ?: throw IllegalArgumentException("Orașul $cityName nu a fost găsit.")

        val encodedTimezone = URLEncoder.encode(location.timezone, StandardCharsets.UTF_8.toString())

        val url = URL(
            "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=${location.latitude}" +
                    "&longitude=${location.longitude}" +
                    "&hourly=temperature_2m" +
                    "&forecast_days=1" +
                    "&timezone=$encodedTimezone"
        )

        val root = JSONObject(url.readText())
        val hourly = root.getJSONObject("hourly")
        val times = hourly.getJSONArray("time")
        val temperatures = hourly.getJSONArray("temperature_2m")

        return (0 until times.length()).map { i ->
            WeatherPoint(
                city = location.name,
                time = times.getString(i),
                temperature = temperatures.getDouble(i)
            )
        }
    }
}
