package com.sd.laborator.services

import com.sd.laborator.interfaces.IWeatherFetchService
import com.sd.laborator.pojo.GeoLocation
import org.json.JSONObject
import org.springframework.stereotype.Service
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Service
class OpenMeteoWeatherFetchService : IWeatherFetchService {

    override fun fetchTemperatures(location: GeoLocation, hours: Int): Pair<List<String>, List<Double>> {
        val encodedTz = URLEncoder.encode(location.timezone, StandardCharsets.UTF_8.toString())
        val root = JSONObject(
            URL(
                "https://api.open-meteo.com/v1/forecast" +
                        "?latitude=${location.latitude}" +
                        "&longitude=${location.longitude}" +
                        "&hourly=temperature_2m" +
                        "&forecast_days=1" +
                        "&timezone=$encodedTz"
            ).readText()
        )

        val hourly = root.getJSONObject("hourly")
        val timesArray = hourly.getJSONArray("time")
        val tempsArray = hourly.getJSONArray("temperature_2m")

        val limit = minOf(hours, timesArray.length())
        val times = (0 until limit).map { timesArray.getString(it) }
        val temps = (0 until limit).map { tempsArray.getDouble(it) }

        return Pair(times, temps)
    }
}
