interface IStatisticsWriter {
    fun write(errors: List<ErrorEvent>, outputPath: String)
}
