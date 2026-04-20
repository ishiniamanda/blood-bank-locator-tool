public class Donor {
    String name;
    String bloodType;
    double lat;
    double lon;
    int supply;
    int demand; 

    public Donor(String name, String bloodType, double lat, double lon, int supply, int demand) {
        this.name = name;
        this.bloodType = bloodType;
        this.lat = lat;
        this.lon = lon;
        this.supply = supply;
        this.demand = demand;
    }
}