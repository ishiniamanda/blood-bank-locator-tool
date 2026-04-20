public class EmergencyRequest implements Comparable<EmergencyRequest> {
    private String patientName;
    private String bloodType;
    private int priority; // 3 = Critical, 2 = Urgent, 1 = Normal

    public EmergencyRequest(String name, String bloodType, int priority) {
        this.patientName = name;
        this.bloodType = bloodType;
        this.priority = priority;
    }

    // This method is used by the Heap to sort the requests
    @Override
    public int compareTo(EmergencyRequest other) {
        return Integer.compare(other.priority, this.priority); // Higher number = Higher priority
    }

    @Override
    public String toString() {
        String level = (priority == 3) ? "CRITICAL" : (priority == 2) ? "URGENT" : "NORMAL";
        return "[" + level + "] " + patientName + " (" + bloodType + ")";
    }
}
