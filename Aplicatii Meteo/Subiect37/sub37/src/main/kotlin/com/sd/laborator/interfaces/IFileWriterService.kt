package com.sd.laborator.interfaces

import com.sd.laborator.pojo.WeatherChainContext

interface IFileWriterService {
    fun write(context: WeatherChainContext): String  // returneaza calea fisierului scris
}
