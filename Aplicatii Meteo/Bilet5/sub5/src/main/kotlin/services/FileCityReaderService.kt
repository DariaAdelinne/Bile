package com.sd.laborator.services

import com.sd.laborator.interfaces.CityReaderInterface
import org.springframework.stereotype.Service
import java.io.File

@Service
class FileCityReaderService : CityReaderInterface {

    override fun readCities(): List<String> {
        val file = File("src/main/resources/cities.txt")

        if (!file.exists()) {
            throw IllegalArgumentException(
                "Fisierul src/main/resources/cities.txt nu exista."
            )
        }

        return file.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}