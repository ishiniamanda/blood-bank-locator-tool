import java.util.*;

public class BloodFilter {

    private static final Map<String, List<String>> compatibility = new HashMap<>();

    static {
        compatibility.put("O-", Arrays.asList("O-"));
        compatibility.put("O+", Arrays.asList("O-", "O+"));
        compatibility.put("A-", Arrays.asList("O-", "A-"));
        compatibility.put("A+", Arrays.asList("O-", "O+", "A-", "A+"));
        compatibility.put("B-", Arrays.asList("O-", "B-"));
        compatibility.put("B+", Arrays.asList("O-", "O+", "B-", "B+"));
        compatibility.put("AB-", Arrays.asList("O-", "A-", "B-", "AB-"));
        compatibility.put("AB+", Arrays.asList("O-", "O+", "A-", "A+", "B-", "B-", "AB-"));
    }

    public static boolean isCompatible(String patient, String donor) {

        if (patient == null || donor == null) return false;

        patient = patient.trim().toUpperCase();
        donor = donor.trim().toUpperCase();

        List<String> allowed = compatibility.get(patient);

        if (allowed == null) return false;

        return allowed.contains(donor);
    }
}