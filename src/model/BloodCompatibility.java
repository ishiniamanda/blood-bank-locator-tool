package model;

import java.util.*;

public class BloodCompatibility {

    private static final Map<String, List<String>> map = new HashMap<>();

    static {
        map.put("A+", Arrays.asList("A+", "A-", "O+", "O-"));
        map.put("A-", Arrays.asList("A-", "O-"));
        map.put("B+", Arrays.asList("B+", "B-", "O+", "O-"));
        map.put("B-", Arrays.asList("B-", "O-"));
        map.put("O+", Arrays.asList("O+", "O-"));
        map.put("O-", Arrays.asList("O-"));
        map.put("AB+", Arrays.asList("A+", "B+", "AB+", "O+", "A-", "B-", "O-"));
    }

    public static List<String> getCompatible(String type) {
        return map.getOrDefault(type, new ArrayList<>());
    }
}