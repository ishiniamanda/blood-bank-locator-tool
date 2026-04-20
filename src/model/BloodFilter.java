package model;

import java.util.*;

public class BloodFilter {

    private Map<String, String> hospitalBlood = new HashMap<>();

    public void addHospital(String hospital, String blood) {
        hospitalBlood.put(hospital, blood);
    }

    public List<String> getHospitalsWithBlood(String requestedBlood) {

        List<String> result = new ArrayList<>();
        List<String> compatible = BloodCompatibility.getCompatible(requestedBlood);

        for (Map.Entry<String, String> entry : hospitalBlood.entrySet()) {

            String hospital = entry.getKey();
            String blood = entry.getValue();

            if (compatible.contains("ALL") || compatible.contains(blood)) {
                result.add(hospital);
            }
        }

        return result;
    }
}