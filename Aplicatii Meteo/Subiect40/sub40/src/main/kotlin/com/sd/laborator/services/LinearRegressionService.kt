package com.sd.laborator.services

import com.sd.laborator.interfaces.ILinearRegressionService
import org.springframework.stereotype.Service

@Service
class LinearRegressionService : ILinearRegressionService {

    // Calculeaza coeficientii a si b pentru y = a*x + b
    // x = indicele orei (0, 1, 2, ..., n-1), y = temperatura
    override fun compute(yValues: List<Double>): Pair<Double, Double> {
        val n = yValues.size
        val xValues = (0 until n).map { it.toDouble() }

        val meanX = xValues.average()
        val meanY = yValues.average()

        var numerator = 0.0
        var denominator = 0.0

        for (i in 0 until n) {
            numerator += (xValues[i] - meanX) * (yValues[i] - meanY)
            denominator += (xValues[i] - meanX) * (xValues[i] - meanX)
        }

        val a = if (denominator != 0.0) numerator / denominator else 0.0
        val b = meanY - a * meanX

        return Pair(a, b)
    }
}
