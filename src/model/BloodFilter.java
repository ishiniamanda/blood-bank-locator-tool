package model;

import java.util.*;

public class BloodFilter {

    private Map<String, List<String>> data = new HashMap<>();

    public BloodFilter() {
        data.put("A+", Arrays.asList("B", "E"));
        data.put("O+", Arrays.asList("A", "C"));
        data.put("B+", Arrays.asList("E"));
    }

    public List<String> getHospitalsWithBlood(String type) {
        return data.getOrDefault(type, new ArrayList<>());
    }
}
