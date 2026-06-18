class ErrorClassifier : IErrorClassifier {
    override fun classify(rawError: String): ErrorEvent {
        return when {
            rawError.contains("DUPLICATE_BID", ignoreCase = true) -> ErrorEvent.deserialize(rawError)
            rawError.contains("BIDDER_DISCONNECTED", ignoreCase = true) -> ErrorEvent.deserialize(rawError)
            rawError.contains("INVALID_MESSAGE", ignoreCase = true) -> ErrorEvent.deserialize(rawError)
            rawError.contains("|") -> ErrorEvent.deserialize(rawError)
            else -> ErrorEvent(ErrorType.UNKNOWN, rawError)
        }
    }
}
