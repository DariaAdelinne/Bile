package com.sd.laborator.statemachine

enum class MachineState {
    SUBMITTED,    // date introduse prin REST, asteapta validare
    VALIDATING,   // functia de validare verifica datele
    PROCESSING,   // functia de procesare executa logica
    COMPLETED,    // functia de finalizare marcheaza succesul
    ERROR         // functia de eroare gestioneaza esecul
}
