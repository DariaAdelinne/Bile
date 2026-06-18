enum class ErrorType(val description: String) {
    DUPLICATE_BID("Oferta duplicata"),
    BIDDER_DISCONNECTED("Bidder deconectat neasteptat"),
    INVALID_MESSAGE("Mesaj invalid / corupt"),
    UNKNOWN("Eroare necunoscuta")
}
