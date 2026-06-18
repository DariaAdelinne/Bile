package com.sd.laborator.services

import com.sd.laborator.interfaces.WeatherForecastInterface
import com.sd.laborator.pojo.WeatherForecastData
import org.json.JSONObject
import org.springframework.stereotype.Service
import java.net.URL

@Service
class WeatherForecastService : WeatherForecastInterface {

    override fun getForecastData(
        locationName: String,
        latitude: Double,
        longitude: Double
    ): WeatherForecastData? {

        // URL pentru API meteo
        val url = URL(
            "https://api.open-meteo.com/v1/forecast?" +
                    "latitude=$latitude&longitude=$longitude" +
                    "&current_weather=true" +
                    "&daily=temperature_2m_max,temperature_2m_min" +
                    "&timezone=auto"
        )

        val rawResponse = url.readText()
        val json = JSONObject(rawResponse)

        if (!json.has("current_weather")) return null

        val current = json.getJSONObject("current_weather")
        val daily = json.getJSONObject("daily")

        val maxTemp = daily.getJSONArray("temperature_2m_max").getDouble(0)
        val minTemp = daily.getJSONArray("temperature_2m_min").getDouble(0)

        val currentTemp = current.getDouble("temperature")
        val windSpeed = current.getDouble("windspeed")
        val weatherCode = current.getInt("weathercode")

        val weatherState = mapWeatherCode(weatherCode)

        return WeatherForecastData(
            location = locationName,
            date = java.time.LocalDateTime.now().toString(),
            weatherState = weatherState,
            weatherStateIconURL = "",
            windDirection = "N/A",
            windSpeed = windSpeed,
            minTemp = minTemp,
            maxTemp = maxTemp,
            currentTemp = currentTemp,
            humidity = 0
        )
    }

    // Convertim codul meteo in text
    private fun mapWeatherCode(code: Int): String {
        return when (code) {
            0 -> "Clear"
            1, 2, 3 -> "Cloudy"
            45 -> "Fog"
            61 -> "Rain"
            71 -> "Snow"
            else -> "Unknown"
        }
    }
}