package com.sd.laborator.interfaces

interface ILinearRegressionService {
    // returneaza Pair(a, b) pentru dreapta y = a*x + b
    fun compute(yValues: List<Double>): Pair<Double, Double>
}
