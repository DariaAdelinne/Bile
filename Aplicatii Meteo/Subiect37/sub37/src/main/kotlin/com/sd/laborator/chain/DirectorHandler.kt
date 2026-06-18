package com.sd.laborator.chain

import com.sd.laborator.interfaces.IFileWriterService
import com.sd.laborator.pojo.WeatherChainContext

// Veriga 3 - MODEL DIRIJOR: decide ce serviciu de scriere sa foloseasca si coordoneaza scrierea
class DirectorHandler(
    private val fileWriterService: IFileWriterService
) : AbstractWeatherChainHandler() {

    override fun handle(context: WeatherChainContext): WeatherChainContext {
        // dirijorul decide catre ce serviciu de scriere se duc datele filtrate
        val filePath = fileWriterService.write(context)

        return passToNext(context.copy(outputFilePath = filePath))
    }
}
