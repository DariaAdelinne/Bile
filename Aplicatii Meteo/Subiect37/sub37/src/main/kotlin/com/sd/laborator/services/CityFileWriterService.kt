package com.sd.laborator.services

import com.sd.laborator.interfaces.IFileWriterService
import com.sd.laborator.pojo.WeatherChainContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File

// Scrie intr-un fisier TXT cu numele orasului: {outputDir}/{cityName}.txt
@Service
class CityFileWriterService(
    @Value("\${weather.output.dir}") private val outputDir: String
) : IFileWriterService {

    override fun write(context: WeatherChainContext): String {
        val dir = File(outputDir)
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, "${context.cityName}.txt")

        val content = buildString {
            appendLine("=== Date meteo pentru ${context.cityName} ===")
            appendLine("Tara: ${context.location?.country ?: "N/A"}")
            appendLine("Temperatura minima: ${context.minTemp}°C")
            appendLine("Temperatura maxima: ${context.maxTemp}°C")
            appendLine()
            appendLine("--- Date orare (${context.filteredData.size} inregistrari) ---")
            context.filteredData.forEach { point ->
                appendLine("${point.time}  ->  ${point.temperature}°C")
            }
        }

        file.writeText(content)
        return file.absolutePath
    }
}
