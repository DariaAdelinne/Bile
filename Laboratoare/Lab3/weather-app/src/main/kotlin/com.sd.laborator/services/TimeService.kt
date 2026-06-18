package com.sd.laborator.services

import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class TimeService {

    // Returneaza data si ora curenta
    fun getCurrentTime(): String {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
        return LocalDateTime.now().format(formatter)
    }
}