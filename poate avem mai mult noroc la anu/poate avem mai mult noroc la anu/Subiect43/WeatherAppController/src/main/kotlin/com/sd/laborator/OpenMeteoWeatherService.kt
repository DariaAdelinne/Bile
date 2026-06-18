package com.sd.laborator

import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs

/**
 * Un "element citit" de vreme (POJO).
 *   source = "open-meteo" (din API) sau "local-fallback" (cand nu e internet la examen).
 */
data class WeatherReading(
    val city: String,
    val country: String,
    val temperature: Double,
    val windspeed: Double,
    val description: String,
    val time: String,
    val source: String
)

/**
 * OpenMeteoWeatherService - serviciul intern de date meteo (din Laboratorul 3, incapsulat).
 *
 * Geocodare (oras -> lat/lon) + vreme curenta, prin Open-Meteo (gratuit, FARA cheie). Inlocuieste
 * metaweather.com din laborator (inchis in 2022).
 *
 * Daca API-ul Open-Meteo nu e accesibil (fara internet la examen), foloseste un FALLBACK LOCAL
 * determinist, ca aplicatia sa mearga oricum (datele sunt marcate cu source="local-fallback").
 *
 * SOLID(S): singura responsabilitate = obtinerea datelor meteo pentru un oras.
 */
class OpenMeteoWeatherService {

    /** Returneaza datele meteo pentru [city], sau null daca orasul chiar nu exista (cu internet). */
    fun getWeather(city: String): WeatherReading? {
        return try {
            val coord = geocode(city) ?: return localFallback(city) // fara internet -> fallback
            fetchWeather(city, coord)
        } catch (e: Exception) {
            println("[WeatherService] Open-Meteo indisponibil ($e) -> fallback local.")
            localFallback(city)
        }
    }

    private data class Coord(val name: String, val country: String, val lat: Double, val lon: Double, val notFound: Boolean = false)

    private fun geocode(city: String): Coord? {
        val enc = URLEncoder.encode(city, "UTF-8")
        val url = URL("https://geocoding-api.open-meteo.com/v1/search?name=$enc&count=1&language=en&format=json")
        val raw = url.readText()
        val results = JSONObject(raw).optJSONArray("results")
        if (results == null || results.isEmpty) return Coord("", "", 0.0, 0.0, notFound = true)
        val first = results.getJSONObject(0)
        return Coord(
            first.getString("name"),
            first.optString("country", "?"),
            first.getDouble("latitude"),
            first.getDouble("longitude")
        )
    }

    private fun fetchWeather(city: String, coord: Coord): WeatherReading? {
        if (coord.notFound) return null  // oras inexistent (avem internet, dar geocodarea nu l-a gasit)
        val url = URL(
            "https://api.open-meteo.com/v1/forecast?latitude=${coord.lat}&longitude=${coord.lon}&current_weather=true"
        )
        val cw = JSONObject(url.readText()).optJSONObject("current_weather") ?: return localFallback(city)
        return WeatherReading(
            city = coord.name,
            country = coord.country,
            temperature = cw.getDouble("temperature"),
            windspeed = cw.getDouble("windspeed"),
            description = describe(cw.getInt("weathercode")),
            time = cw.getString("time"),
            source = "open-meteo"
        )
    }

    /** Date locale deterministe (in functie de numele orasului), ca demo-ul sa mearga fara internet. */
    private fun localFallback(city: String): WeatherReading {
        val h = abs(city.lowercase().hashCode())
        val temp = (h % 35) - 5.0          // -5 .. 29 grade
        val wind = (h % 20) + 1.0          // 1 .. 20 km/h
        val codes = intArrayOf(0, 1, 2, 3, 45, 61, 71, 80, 95)
        val code = codes[h % codes.size]
        return WeatherReading(
            city = city.replaceFirstChar { it.uppercase() },
            country = "demo",
            temperature = temp,
            windspeed = wind,
            description = describe(code),
            time = "demo",
            source = "local-fallback"
        )
    }

    /** Mapare cod WMO (Open-Meteo) -> descriere in romana. */
    private fun describe(code: Int): String = when (code) {
        0 -> "Senin"
        1, 2, 3 -> "Partial noros"
        45, 48 -> "Ceata"
        in 51..57 -> "Burnita"
        in 61..67 -> "Ploaie"
        in 71..77 -> "Ninsoare"
        in 80..82 -> "Averse"
        in 95..99 -> "Furtuna"
        else -> "Necunoscut ($code)"
    }
}
