public class Donor {

    private String name;
    private String bloodType;
    private double lat, lon;

    private int age;
    private double weight;
    private double hemoglobin;
    private boolean isPregnant;
    
    // ADD THIS LINE
    public boolean isHospital; 

    // Constructor for Hospitals
    public Donor(String name, String bloodType, double lat, double lon) {
        this.name = name;
        this.bloodType = bloodType;
        this.lat = lat;
        this.lon = lon;
        this.isHospital = true; // Hospitals are true
    }

    // Constructor for Individuals
    public Donor(String name, String bloodType, double lat, double lon,
                 int age, double weight, double hemoglobin, boolean isPregnant) {

        this(name, bloodType, lat, lon);
        this.age = age;
        this.weight = weight;
        this.hemoglobin = hemoglobin;
        this.isPregnant = isPregnant;
        this.isHospital = false; // Individuals are false
    }

    @Override
    public String toString() {
        return name + "," + bloodType + "," + lat + "," + lon + "," +
               age + "," + weight + "," + hemoglobin + "," + isPregnant + "," + isHospital;
    }

    public String getName() { return name; }
    public String getBloodType() { return bloodType; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }
}