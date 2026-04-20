import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class MapDashboard extends Application {
    
    private RedBlackTree donorTree;

    @Override
    public void start(Stage stage) {
        // 1. Initialize Database
        donorTree = new RedBlackTree();
        donorTree.insert(new Donor("National Blood Center (Colombo)", "A+", 6.9054, 79.8731));
        donorTree.insert(new Donor("Negombo General Hospital", "O-", 7.2091, 79.8485));
        donorTree.insert(new Donor("Kandy Teaching Hospital", "O+", 7.2906, 80.6337));
        donorTree.insert(new Donor("Karapitiya Hospital (Galle)", "B-", 6.0667, 80.2250));
        donorTree.insert(new Donor("Jaffna Teaching Hospital", "O-", 9.6615, 80.0255));
        donorTree.insert(new Donor("Batticaloa Teaching Hospital", "AB-", 7.7102, 81.6924));
        donorTree.insert(new Donor("Kurunegala Hospital", "A+", 7.4818, 80.3609));
        donorTree.insert(new Donor("Anuradhapura Hospital", "B+", 8.3450, 80.4100));
        donorTree.insert(new Donor("Badulla Provincial Hospital", "O+", 6.9934, 81.0550));
        donorTree.insert(new Donor("Ratnapura Teaching Hospital", "A-", 6.6828, 80.3992));

        // 2. Setup UI Inputs
        Label titleLabel = new Label("Emergency Blood Bank Locator");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setStyle("-fx-text-fill: #2c3e50;");

        TextField bloodField = new TextField();
        bloodField.setPromptText("Enter Blood Type (e.g. O+)");
        bloodField.setStyle("-fx-font-size: 14px; -fx-padding: 10px;");

        TextField cityField = new TextField();
        cityField.setPromptText("Enter Your City (e.g. Jaffna)");
        cityField.setStyle("-fx-font-size: 14px; -fx-padding: 10px;");
        
        Button searchButton = new Button("SEARCH DONORS");
        searchButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10px 20px; -fx-cursor: hand;");

        // 3. Setup Results Area
        VBox resultsBox = new VBox(15);
        resultsBox.setAlignment(Pos.CENTER);
        resultsBox.setPadding(new Insets(20));
        resultsBox.setStyle("-fx-background-color: #ecf0f1; -fx-background-radius: 10;");

        // 4. Search Logic
        searchButton.setOnAction(e -> {
            resultsBox.getChildren().clear(); // Clear previous results
            String type = bloodField.getText().toUpperCase().trim();
            String city = cityField.getText().trim();
            
            Donor found = donorTree.search(type);

            if (found != null && !city.isEmpty()) {
                Label loadingLabel = new Label("Locating " + city + "...");
                resultsBox.getChildren().add(loadingLabel);
                
                double[] cityCoords = getCoordinatesFromCity(city);
                resultsBox.getChildren().clear(); 
                
                if (cityCoords != null) {
                    // CALCULATE DISTANCE
                    double distance = calculateDistance(cityCoords[0], cityCoords[1], found.lat, found.lon);
                    
                    Label matchLabel = new Label("Donor Found: " + found.name);
                    matchLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
                    matchLabel.setStyle("-fx-text-fill: #27ae60;");

                    Label distanceLabel = new Label(String.format("Approximate Distance: %.1f km", distance));
                    distanceLabel.setFont(Font.font("Arial", 14));

                    // GOOGLE MAPS LINK GENERATION
                    Button mapButton = new Button("View Route in Google Maps \uD83D\uDDFA\uFE0F");
                    mapButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 20px; -fx-cursor: hand;");
                    
                    String googleMapsUrl = String.format("https://www.google.com/maps/dir/?api=1&origin=%f,%f&destination=%f,%f", 
                                                         cityCoords[0], cityCoords[1], found.lat, found.lon);
                    
                    mapButton.setOnAction(ev -> {
                        // This command opens the user's default web browser!
                        getHostServices().showDocument(googleMapsUrl);
                    });

                    resultsBox.getChildren().addAll(matchLabel, distanceLabel, mapButton);
                } else {
                    resultsBox.getChildren().add(new Label("Error: Could not locate your city."));
                }
            } else {
                resultsBox.getChildren().add(new Label("No " + type + " donor found in the system."));
            }
        });

        // 5. Layout
        VBox mainLayout = new VBox(20);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setPadding(new Insets(40));
        mainLayout.getChildren().addAll(titleLabel, bloodField, cityField, searchButton, resultsBox);
        
        Scene scene = new Scene(mainLayout, 600, 500);
        stage.setTitle("Emergency Blood Bank Locator Tracker");
        stage.setScene(scene);
        stage.show();
    }

    // --- ALGORITHM: Haversine Formula for Distance Calculation ---
    // This looks great in your project report!
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the Earth in kilometers
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
                 
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // Returns distance in KM
    }

    // --- API: City Geocoding ---
    private double[] getCoordinatesFromCity(String cityName) {
        try {
            String safeCityName = java.net.URLEncoder.encode(cityName.trim(), "UTF-8");
            String urlStr = "https://nominatim.openstreetmap.org/search?q=" + safeCityName + "%2CSri+Lanka&format=json&limit=1";
            
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "NIBM_Student_Project/1.0"); 

            Scanner scanner = new Scanner(conn.getInputStream());
            if (!scanner.hasNext()) return null;
            String response = scanner.useDelimiter("\\A").next();
            scanner.close();

            if (response.contains("\"lat\":\"")) {
                double lat = Double.parseDouble(response.split("\"lat\":\"")[1].split("\"")[0]);
                double lon = Double.parseDouble(response.split("\"lon\":\"")[1].split("\"")[0]);
                return new double[]{lat, lon};
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}