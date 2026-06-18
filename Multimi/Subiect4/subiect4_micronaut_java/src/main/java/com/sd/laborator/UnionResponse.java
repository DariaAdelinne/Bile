package com.sd.laborator;

import io.micronaut.core.annotation.Introspected;
import java.util.ArrayList;
import java.util.List;

@Introspected
public class UnionResponse {
    private String message;
    private List<Integer> aValues = new ArrayList<>();
    private List<Integer> bValues = new ArrayList<>();
    private List<Integer> cValues = new ArrayList<>();

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

    public List<Integer> getBValues() {
        return bValues;
    }

    public void setBValues(List<Integer> bValues) {
        this.bValues = bValues;
    }

    public List<Integer> getCValues() {
        return cValues;
    }

    public void setCValues(List<Integer> cValues) {
        this.cValues = cValues;
    }
}
