interface IErrorClassifier {
    fun classify(rawError: String): ErrorEvent
}
