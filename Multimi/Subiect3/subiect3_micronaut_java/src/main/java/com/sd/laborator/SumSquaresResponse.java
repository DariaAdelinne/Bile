package com.sd.laborator;

import io.micronaut.core.annotation.Introspected;
import java.util.ArrayList;
import java.util.List;

@Introspected
public class SumSquaresResponse {
    private String message;
    private List<Integer> aValues = new ArrayList<>();
    private List<Long> bValues = new ArrayList<>();

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Integer> getAValues() {
        return aValues;
    }

    public void setAValues(List<Integer> aValues) {
        this.aValues = aValues;
    }

    public List<Long> getBValues() {
        return bValues;
    }

    public void setBValues(List<Long> bValues) {
        this.bValues = bValues;
    }
}
