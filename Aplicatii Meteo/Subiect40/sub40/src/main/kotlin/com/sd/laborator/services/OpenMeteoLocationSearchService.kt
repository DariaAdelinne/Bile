package com.sd.laborator.services

import com.sd.laborator.interfaces.ILocationSearchService
import com.sd.laborator.pojo.GeoLocation
import org.json.JSONObject
import org.springframework.stereotype.Service
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Service
class OpenMeteoLocationSearchService : ILocationSearchService {

    override fun getLocation(cityName: String): GeoLocation? {
        val encoded = URLEncoder.encode(cityName, StandardCharsets.UTF_8.toString())
        val root = JSONObject(
            URL("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=en&format=json")
                .readText()
        )

        if (!root.has("results")) return null
        val results = root.getJSONArray("results")
        if (results.isEmpty) return null

        val first = results.getJSONObject(0)
        return GeoLocation(
            name = first.getString("name"),
            country = first.optString("country", "Unknown"),
            latitude = first.getDouble("latitude"),
            longitude = first.getDouble("longitude"),
            timezone = first.optString("timezone", "auto")
        )
    }
}
