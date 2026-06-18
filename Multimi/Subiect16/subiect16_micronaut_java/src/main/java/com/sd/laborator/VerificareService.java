package com.sd.laborator;

import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
public class VerificareService {

    public boolean verificaPereche(int a, int b) {
        return a * b == a + b * 3;
    }

    public List<Map<String, Integer>> gasestePerechile(List<Integer> adtA, List<Integer> adtB) {
        List<Map<String, Integer>> rezultate = new ArrayList<>();
        Set<String> vazute = new LinkedHashSet<>();

        for (int a : adtA) {
            for (int b : adtB) {
                String cheie = a + "," + b;
                if (verificaPereche(a, b) && vazute.add(cheie)) {
                    rezultate.add(Map.of("a", a, "b", b));
                }
            }
        }

        return rezultate;
    }
}
