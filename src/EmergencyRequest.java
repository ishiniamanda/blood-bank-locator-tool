public class EmergencyRequest implements Comparable<EmergencyRequest> {
    String description;
    String bloodType;
    int priority; // 3 = CRITICAL, 2 = URGENT, 1 = NORMAL

    public EmergencyRequest(String description, String bloodType, int priority) {
        this.description = description;
        this.bloodType = bloodType;
        this.priority = priority;
    }

    @Override
    public int compareTo(EmergencyRequest other) {
        // Higher priority numbers go to the top of the queue
        return Integer.compare(other.priority, this.priority);
    }

    @Override
    public String toString() {
        String level = priority == 3 ? "[CRITICAL]" : (priority == 2 ? "[URGENT]" : "[NORMAL]");
        return level + " Need " + bloodType + " - " + description;
    }
}