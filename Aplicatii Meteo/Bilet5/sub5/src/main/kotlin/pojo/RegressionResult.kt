package com.sd.laborator.pojo

data class RegressionResult(
    val city: String,
    val slope: Double,
    val angleDegrees: Double,
    val numberOfPoints: Int,
    val message: String
)