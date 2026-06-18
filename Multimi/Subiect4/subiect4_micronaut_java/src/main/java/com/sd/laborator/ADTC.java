package com.sd.laborator;

import io.micronaut.core.annotation.Introspected;
import java.util.ArrayList;
import java.util.List;

@Introspected
public class ADTC {
    private final List<Integer> values = new ArrayList<>();

    public void addValue(int value) {
        values.add(value);
    }

    public List<Integer> getValues() {
        return values;
    }
}
