package com.sd.laborator;

import io.micronaut.core.annotation.Introspected;
import java.util.ArrayList;
import java.util.List;

@Introspected
public class ADTB {
    private final List<Long> results = new ArrayList<>();

    public void addResult(long value) {
        results.add(value);
    }

    public List<Long> getResults() {
        return results;
    }
}
