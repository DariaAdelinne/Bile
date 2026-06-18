package com.sd.laborator;

import io.micronaut.core.annotation.Introspected;

@Introspected
public class UnionRequest {
    private int size = 100;
    private int minValue = 1;
    private int maxValue = 50;

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getMinValue() {
        return minValue;
    }

    public void setMinValue(int minValue) {
        this.minValue = minValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
    }
}
