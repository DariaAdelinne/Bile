package com.sd.laborator.pojo

/**
 * Modelul unei cheltuieli de familie.
 *
 * @param id          identificator unic
 * @param descriere   descrierea cheltuielii (ex: "curent", "mancare")
 * @param suma        suma in lei
 * @param categorie   categoria (ex: "utilitati", "alimentare", "transport")
 * @param platitDe    membrul familiei care a platit
 */
data class Cheltuiala(
    var id: Int = 0,
    var descriere: String = "",
    var suma: Double = 0.0,
    var categorie: String = "",
    var platitDe: String = ""
)
