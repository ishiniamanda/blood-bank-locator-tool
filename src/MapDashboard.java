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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class MapDashboard extends Application {

    private RedBlackTree donorTree;
    private Graph graph;

    @Override
    public void start(Stage stage) {

        donorTree = new RedBlackTree();
        
        // Database with multiple "O-" and "A+" donors to test the list!
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

        // ================= UI =================
        Label titleLabel = new Label("Emergency Blood Bank Locator");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        titleLabel.setStyle("-fx-text-fill: #1f2d3d;");

        TextField bloodField = new TextField();
        bloodField.setPromptText("Enter Patient Blood Type (e.g. A+)");
        bloodField.setStyle("-fx-font-size: 14px; -fx-padding: 10px;");

        TextField cityField = new TextField();
        cityField.setPromptText("Enter Patient City (e.g. Jaffna)");
        cityField.setStyle("-fx-font-size: 14px; -fx-padding: 10px;");
        
        Button searchButton = new Button("SEARCH DONORS");
        searchButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10px 20px; -fx-cursor: hand;");

        VBox resultsBox = new VBox(15);
        resultsBox.setAlignment(Pos.TOP_CENTER);
        resultsBox.setPadding(new Insets(20));
        resultsBox.setStyle(
                "-fx-background-color: #f5f6fa;" +
                "-fx-border-color: #dcdde1;" +
                "-fx-border-radius: 10;"
        );

        // ================= SEARCH BUTTON =================
        searchButton.setOnAction(e -> {
            resultsBox.getChildren().clear(); 
            String type = bloodField.getText().toUpperCase().trim();
            String city = cityField.getText().trim();
            
            // 1. Get the raw list of matching donors from the Tree
            ArrayList<Donor> matchingDonors = donorTree.search(type);

            if (!matchingDonors.isEmpty() && !city.isEmpty()) {
                Label loadingLabel = new Label("Locating " + city + "...");
                resultsBox.getChildren().add(loadingLabel);
                
                double[] cityCoords = getCoordinatesFromCity(city);
                resultsBox.getChildren().clear();

                if (cityCoords != null) {
                    
                    // 2. Calculate distance for EVERY donor and store them in a temporary list
                    List<DonorResult> sortedResults = new ArrayList<>();
                    for (Donor d : matchingDonors) {
                        double distance = calculateDistance(cityCoords[0], cityCoords[1], d.lat, d.lon);
                        sortedResults.add(new DonorResult(d, distance));
                    }

                    // 3. Sort the list from shortest distance to longest distance
                    sortedResults.sort((a, b) -> Double.compare(a.distance, b.distance));

                    // 4. Update the UI
                    Label summaryLabel = new Label("Found " + sortedResults.size() + " donor(s) near " + city + ":");
                    summaryLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
                    resultsBox.getChildren().add(summaryLabel);

                    VBox hospitalCards = new VBox(10);
                    
                    // Loop through the sorted results and create a UI card for each one!
                    for (DonorResult result : sortedResults) {

                        HBox card = new HBox(15);
                        card.setStyle("-fx-background-color: white; -fx-padding: 15;");
                        card.setAlignment(Pos.CENTER_LEFT);

                        VBox textInfo = new VBox(5);
                        Label nameLabel = new Label(result.donor.name);
                        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                        nameLabel.setStyle("-fx-text-fill: #27ae60;");
                        
                        Label distLabel = new Label(String.format("Approx. Distance: %.1f km", result.distance));
                        distLabel.setFont(Font.font("Arial", 12));
                        
                        textInfo.getChildren().addAll(nameLabel, distLabel);

                        Region spacer = new Region();
                        HBox.setHgrow(spacer, Priority.ALWAYS);

                        Button mapBtn = new Button("Google Maps");

                        String url = String.format(
                                "https://www.google.com/maps/dir/?api=1&origin=%f,%f&destination=%f,%f",
                                cityCoords[0], cityCoords[1], result.donor.lat, result.donor.lon
                        );

                        mapBtn.setOnAction(ev -> getHostServices().showDocument(url));

                        card.getChildren().addAll(textInfo, spacer, mapBtn);
                        hospitalCards.getChildren().add(card);
                    }

                    resultsBox.getChildren().add(hospitalCards);

                } else {
                    resultsBox.getChildren().add(new Label("City not found."));
                }

            } else {
                resultsBox.getChildren().add(new Label("No " + type + " donor found in the system."));
            }
        });

        VBox mainLayout = new VBox(20);
        mainLayout.setAlignment(Pos.TOP_CENTER);
        mainLayout.setPadding(new Insets(40));
        mainLayout.getChildren().addAll(
                titleLabel,
                bloodField,
                cityField,
                searchButton,
                routeButton,
                resultsBox
        );

        Scene scene = new Scene(mainLayout, 650, 600);
        stage.setScene(scene);
        stage.setTitle("Emergency Blood Bank Locator");
        stage.show();
    }

    // --- THE COMPATIBILITY BRAIN ---
    // This medical logic defines who can receive what blood.
    private List<String> getCompatibleBloodTypes(String patientType) {
        switch (patientType) {
            case "O-":  return Arrays.asList("O-"); // Universal Donor, but can only receive O-
            case "O+":  return Arrays.asList("O+", "O-");
            case "A-":  return Arrays.asList("A-", "O-");
            case "A+":  return Arrays.asList("A+", "A-", "O+", "O-");
            case "B-":  return Arrays.asList("B-", "O-");
            case "B+":  return Arrays.asList("B+", "B-", "O+", "O-");
            case "AB-": return Arrays.asList("AB-", "A-", "B-", "O-");
            case "AB+": return Arrays.asList("AB+", "AB-", "A+", "A-", "B+", "B-", "O+", "O-"); // Universal Recipient
            default:    return new ArrayList<>(); // Returns empty if they type something wrong
        }
    }

    // ================= DISTANCE =================
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        return 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) * R;
    }

    // ================= GEO API =================
    private double[] getCoordinatesFromCity(String cityName) {
        try {
            String safeCity = java.net.URLEncoder.encode(cityName.trim(), "UTF-8");
            String urlStr = "https://nominatim.openstreetmap.org/search?q="
                    + safeCity + "&format=json&limit=1";

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "JavaFX-App");

            Scanner sc = new Scanner(conn.getInputStream());
            String response = sc.useDelimiter("\\A").next();
            sc.close();

            if (response.contains("\"lat\":\"")) {
                double lat = Double.parseDouble(response.split("\"lat\":\"")[1].split("\"")[0]);
                double lon = Double.parseDouble(response.split("\"lon\":\"")[1].split("\"")[0]);
                return new double[]{lat, lon};
            }

        } catch (Exception e) {
            return null;
        }

        return null;
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static class DonorResult {
        Donor donor;
        double distance;

        public DonorResult(Donor donor, double distance) {
            this.donor = donor;
            this.distance = distance;
        }
    }
}