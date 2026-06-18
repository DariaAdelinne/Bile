package com.sd.laborator

import com.sd.laborator.services.EratosteneSieveService
import com.sd.laborator.services.RecursiveSequenceService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Lab11Test {
    @Test
    fun testRecursiveSequence() {
        val service = RecursiveSequenceService()
        assertEquals(listOf(1L, 3L, 6L, 10L, 15L, 21L), service.calculateTerms(5))
    }

    @Test
    fun testPrimeFilter() {
        val service = EratosteneSieveService()
        val result = service.filterOnlyRequestedPrimes(50, listOf(1, 2, 3, 4, 5, 8, 11, 12, 17, 50))
        assertEquals(listOf(2, 3, 5, 11, 17), result)
    }
}
