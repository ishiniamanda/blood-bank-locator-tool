public class Volunteer {
    String name;
    String bloodType;
    String phone; 
    double lat;
    double lon;
    int age;
    double weight;
    double hemoglobin;
    boolean isPregnant;

    public Volunteer(String name, String bloodType, String phone, double lat, double lon, int age, double weight, double hemoglobin, boolean isPregnant) {
        this.name = name;
        this.bloodType = bloodType;
        this.phone = phone;
        this.lat = lat;
        this.lon = lon;
        this.age = age;
        this.weight = weight;
        this.hemoglobin = hemoglobin;
        this.isPregnant = isPregnant;
    }
    
    // This helper method makes it incredibly easy to save to your friend's text file!
    public String toCSV() {
        return name + "," + bloodType + "," + phone + "," + lat + "," + lon + "," + age + "," + weight + "," + hemoglobin + "," + isPregnant;
    }
}