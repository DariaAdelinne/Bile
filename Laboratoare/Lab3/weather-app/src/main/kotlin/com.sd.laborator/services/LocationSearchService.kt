package com.sd.laborator.services

import com.sd.laborator.interfaces.LocationData
import com.sd.laborator.interfaces.LocationSearchInterface
import org.json.JSONObject
import org.springframework.stereotype.Service
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Service
class LocationSearchService : LocationSearchInterface {

    override fun getLocation(locationName: String): LocationData? { //Primeste nume de oras si intreaba API-ul de geocoding de la Open_Meteo ca sa afle toate datele

        // Codam numele locatiei pentru URL
        val encodedName = URLEncoder.encode(locationName, StandardCharsets.UTF_8.toString())

        // Construieste URL
        val url = URL("https://geocoding-api.open-meteo.com/v1/search?name=$encodedName&count=1")

        val rawResponse = url.readText() // Primeste JSON si il parseaza
        val json = JSONObject(rawResponse)

        // Verificam daca exista rezultate
        if (!json.has("results")) {
            return null
        }

        val results = json.getJSONArray("results")
        if (results.isEmpty) { // Daca nu gaseste locatia intoarce null
            return null
        }

        val first = results.getJSONObject(0) // Ia prima locatie gasita

        // Returnam coordonatele
        return LocationData(
            name = first.getString("name"),
            latitude = first.getDouble("latitude"),
            longitude = first.getDouble("longitude")
        )
    }
}