package com.sd.laborator;

import io.micronaut.core.annotation.Introspected;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Introspected
public class ADTB {
    private final List<Integer> values = new ArrayList<>();

    public void initializeRandom(int size, int minValue, int maxValue) {
        values.clear();
        Random random = new Random();
        for (int i = 0; i < size; i++) {
            values.add(random.nextInt(maxValue - minValue + 1) + minValue);
        }
    }

    public List<Integer> getValues() {
        return values;
    }
}
