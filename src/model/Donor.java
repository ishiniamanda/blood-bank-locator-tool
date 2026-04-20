package model;

public class Donor {

    private String name;
    private String bloodType;
    private int priority; // lower = more urgent

    public Donor(String name, String bloodType, int priority) {
        this.name = name;
        this.bloodType = bloodType;
        this.priority = priority;
    }

    public String getName() { return name; }
    public String getBloodType() { return bloodType; }
    public int getPriority() { return priority; }
}
