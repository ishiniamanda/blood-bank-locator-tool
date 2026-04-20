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
import java.util.List;
import java.util.Scanner;

public class MapDashboard extends Application {
    
    private RedBlackTree donorTree;

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

        VBox resultsBox = new VBox(15);
        resultsBox.setAlignment(Pos.TOP_CENTER);
        resultsBox.setPadding(new Insets(20));
        resultsBox.setStyle("-fx-background-color: #ecf0f1; -fx-background-radius: 10;");

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
                    summaryLabel.setStyle("-fx-text-fill: #2c3e50;");
                    resultsBox.getChildren().add(summaryLabel);

                    // Create a scrollable list area
                    VBox hospitalCards = new VBox(10);
                    
                    // Loop through the sorted results and create a UI card for each one!
                    for (DonorResult result : sortedResults) {
                        HBox card = new HBox(15);
                        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 8; -fx-border-color: #bdc3c7; -fx-border-radius: 8;");
                        card.setAlignment(Pos.CENTER_LEFT);

                        VBox textInfo = new VBox(5);
                        Label nameLabel = new Label(result.donor.name);
                        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                        nameLabel.setStyle("-fx-text-fill: #27ae60;");
                        
                        Label distLabel = new Label(String.format("Approx. Distance: %.1f km", result.distance));
                        distLabel.setFont(Font.font("Arial", 12));
                        
                        textInfo.getChildren().addAll(nameLabel, distLabel);

                        // Spacer pushes the button to the right side
                        Region spacer = new Region();
                        HBox.setHgrow(spacer, Priority.ALWAYS);

                        Button mapBtn = new Button("Google Maps \uD83D\uDDFA\uFE0F");
                        mapBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
                        
                        String googleMapsUrl = String.format("https://www.google.com/maps/dir/?api=1&origin=%f,%f&destination=%f,%f", 
                                                             cityCoords[0], cityCoords[1], result.donor.lat, result.donor.lon);
                        mapBtn.setOnAction(ev -> getHostServices().showDocument(googleMapsUrl));

                        card.getChildren().addAll(textInfo, spacer, mapBtn);
                        hospitalCards.getChildren().add(card);
                    }

                    ScrollPane scrollPane = new ScrollPane(hospitalCards);
                    scrollPane.setFitToWidth(true);
                    scrollPane.setStyle("-fx-background-color: transparent; -fx-background: #ecf0f1;");
                    scrollPane.setPrefHeight(250);

                    resultsBox.getChildren().add(scrollPane);

                } else {
                    resultsBox.getChildren().add(new Label("Error: Could not locate your city."));
                }
            } else {
                resultsBox.getChildren().add(new Label("No " + type + " donor found in the system."));
            }
        });

        VBox mainLayout = new VBox(20);
        mainLayout.setAlignment(Pos.TOP_CENTER);
        mainLayout.setPadding(new Insets(40));
        mainLayout.getChildren().addAll(titleLabel, bloodField, cityField, searchButton, resultsBox);
        
        Scene scene = new Scene(mainLayout, 650, 600);
        stage.setTitle("Emergency Blood Bank Locator Tracker");
        stage.setScene(scene);
        stage.show();
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; 
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
                 
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; 
    }

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

    // --- NEW HELPER CLASS ---
    // This pairs a Donor with their calculated distance so we can sort them!
    private static class DonorResult {
        Donor donor;
        double distance;

        public DonorResult(Donor donor, double distance) {
            this.donor = donor;
            this.distance = distance;
        }
    }
}