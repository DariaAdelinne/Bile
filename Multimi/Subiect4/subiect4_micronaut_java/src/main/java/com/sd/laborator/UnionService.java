package com.sd.laborator;

import jakarta.inject.Singleton;
import java.util.HashSet;
import java.util.Set;

@Singleton
public class UnionService {
    public void calculate(ADTA adta, ADTB adtb, ADTC adtc) {
        Set<Integer> seen = new HashSet<>();
        for (Integer value : adta.getValues()) {
            if (seen.add(value)) {
                adtc.addValue(value);
            }
        }
        for (Integer value : adtb.getValues()) {
            if (seen.add(value)) {
                adtc.addValue(value);
            }
        }
    }
}
