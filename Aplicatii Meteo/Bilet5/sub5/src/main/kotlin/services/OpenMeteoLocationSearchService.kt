package com.sd.laborator.services

import com.sd.laborator.interfaces.LocationSearchInterface
import com.sd.laborator.pojo.GeoLocation
import org.json.JSONObject
import org.springframework.stereotype.Service
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Service
class OpenMeteoLocationSearchService : LocationSearchInterface {

    override fun getLocation(cityName: String): GeoLocation? {
        val encodedCityName = URLEncoder.encode(
            cityName,
            StandardCharsets.UTF_8.toString()
        )

        val url = URL(
            "https://geocoding-api.open-meteo.com/v1/search" +
                    "?name=$encodedCityName&count=1&language=en&format=json"
        )

        val rawResponse = url.readText()
        val root = JSONObject(rawResponse)

        if (!root.has("results")) {
            return null
        }

        val results = root.getJSONArray("results")

        if (results.isEmpty) {
            return null
        }

        val firstResult = results.getJSONObject(0)

        return GeoLocation(
            name = firstResult.getString("name"),
            country = firstResult.optString("country", "Unknown"),
            latitude = firstResult.getDouble("latitude"),
            longitude = firstResult.getDouble("longitude"),
            timezone = firstResult.optString("timezone", "auto")
        )
    }
}