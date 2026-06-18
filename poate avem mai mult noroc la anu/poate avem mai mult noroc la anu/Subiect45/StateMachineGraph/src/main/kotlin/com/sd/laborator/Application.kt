package com.sd.laborator

import io.micronaut.runtime.Micronaut

/** Constanta cu numele cozii RabbitMQ (folosita si de producator si de consumator). */
const val STATE_QUEUE = "state-machine-queue"

/**
 * Aplicatia "state machine prin coregrafie de functii serverless".
 *
 * Un singur serviciu Micronaut care contine:
 *  - un REST (TransitionController) la care userul face POST -> pune mesajul intr-o coada RabbitMQ;
 *  - o FUNCTIE SERVERLESS (StateMachineFunction, @RabbitListener) care ASCULTA coada (nu o ruta),
 *    destructureaza mesajul, il impacheteaza in NodeRequest, aplica logica de graf (NodeGraphService)
 *    si pune rezultatul intr-un NodeResponse.
 */
object Application {
    @JvmStatic
    fun main(args: Array<String>) {
        Micronaut.build()
            .args(*args)
            .packages("com.sd.laborator")
            .start()
    }
}
