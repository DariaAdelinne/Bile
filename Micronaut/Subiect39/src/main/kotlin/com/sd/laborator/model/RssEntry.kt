package com.sd.laborator.model

/**
 * Model de date pentru o intrare din fluxul RSS/Atom.
 *
 * Principiul S (SRP): encapsuleaza exclusiv datele unei intrari RSS,
 * fara logica de parsare sau afisare.
 */
data class RssEntry(
    val title: String,
    val url: String
) {
    override fun toString(): String = "<$title, $url>"
}
