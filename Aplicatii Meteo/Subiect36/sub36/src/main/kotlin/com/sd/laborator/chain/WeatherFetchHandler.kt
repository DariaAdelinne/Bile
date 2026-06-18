package com.sd.laborator.chain

import com.sd.laborator.pojo.WeatherChainContext
import com.sd.laborator.pojo.WeatherPoint
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// Veriga 2: descarca datele meteo de la OpenMeteo
class WeatherFetchHandler : AbstractWeatherChainHandler() {

    override fun handle(context: WeatherChainContext): WeatherChainContext {
        val location = context.location
            ?: throw IllegalStateException("Locatia nu a fost rezolvata de handlerul anterior.")

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

        val rawData = (0 until times.length()).map { i ->
            WeatherPoint(
                city = location.name,
                time = times.getString(i),
                temperature = temperatures.getDouble(i)
            )
        }

        val updatedContext = context.copy(rawData = rawData)
        return passToNext(updatedContext)
    }
}
