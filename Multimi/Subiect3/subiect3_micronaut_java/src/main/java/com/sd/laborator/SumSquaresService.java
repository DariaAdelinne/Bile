package com.sd.laborator;

import jakarta.inject.Singleton;

@Singleton
public class SumSquaresService {
    public void calculate(ADTA adta, ADTB adtb) {
        long partialSum = 0;
        for (Integer value : adta.getValues()) {
            partialSum += (long) value * value;
            adtb.addResult(partialSum);
        }
    }
}
