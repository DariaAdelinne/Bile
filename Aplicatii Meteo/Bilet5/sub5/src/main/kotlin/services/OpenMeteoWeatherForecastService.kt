package com.sd.laborator.services

import com.sd.laborator.interfaces.WeatherForecastInterface
import com.sd.laborator.pojo.GeoLocation
import com.sd.laborator.pojo.WeatherPoint
import org.json.JSONObject
import org.springframework.stereotype.Service
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Service
class OpenMeteoWeatherForecastService : WeatherForecastInterface {

    override fun getForecastData(location: GeoLocation): List<WeatherPoint> {
        val encodedTimezone = URLEncoder.encode(
            location.timezone,
            StandardCharsets.UTF_8.toString()
        )

        val url = URL(
            "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=${location.latitude}" +
                    "&longitude=${location.longitude}" +
                    "&hourly=temperature_2m" +
                    "&forecast_days=1" +
                    "&timezone=$encodedTimezone"
        )

        val rawResponse = url.readText()
        val root = JSONObject(rawResponse)

        val hourly = root.getJSONObject("hourly")
        val times = hourly.getJSONArray("time")
        val temperatures = hourly.getJSONArray("temperature_2m")

        val result = mutableListOf<WeatherPoint>()

        for (i in 0 until times.length()) {
            result.add(
                WeatherPoint(
                    city = location.name,
                    time = times.getString(i),
                    temperature = temperatures.getDouble(i)
                )
            )
        }

        return result
    }
}