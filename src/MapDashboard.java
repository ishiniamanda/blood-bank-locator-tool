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
import java.util.Scanner;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.PriorityQueue;

public class MapDashboard extends Application {

    private PriorityQueue<EmergencyRequest> requestHeap = new PriorityQueue<>();
    private ListView<String> queueListView = new ListView<>(); // To show the queue in UI
    private RedBlackTree donorTree;

    @Override
    public void start(Stage stage) {

        donorTree = new RedBlackTree();

        // Database
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
        bloodField.setPromptText("Enter Patient Blood Type (e.g. A+)");
        bloodField.setStyle("-fx-font-size: 14px; -fx-padding: 10px;");

        TextField cityField = new TextField();
        cityField.setPromptText("Enter Patient City (e.g. Jaffna)");
        cityField.setStyle("-fx-font-size: 14px; -fx-padding: 10px;");

        Button searchButton = new Button("SEARCH SHORTEST ROUTE");
        searchButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10px 20px; -fx-cursor: hand;");

        Button donorButton = new Button("Become a Donor");

        donorButton.setStyle(
                "-fx-background-color: #27ae60;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 14px;" +
                "-fx-padding: 10px 20px;" +
                "-fx-cursor: hand;"
        );

        donorButton.setOnAction(e -> {
    // Pass 'this' as the second argument
    DonorForm form = new DonorForm(donorTree, this); 
    form.show();
});

        VBox resultsBox = new VBox(15);
        resultsBox.setAlignment(Pos.TOP_CENTER);
        resultsBox.setPadding(new Insets(20));
        resultsBox.setStyle("-fx-background-color: #ecf0f1; -fx-background-radius: 10;");

        searchButton.setOnAction(e -> {

            resultsBox.getChildren().clear();

            String patientType = bloodField.getText().toUpperCase().trim();
            String city = cityField.getText().trim();
            // 1. Create a priority (1 = Normal, 2 = Urgent, 3 = Critical)
    // For now, let's make every search "Urgent" (Level 2)
    EmergencyRequest newReq = new EmergencyRequest("Patient in " + city, patientType, 2);

    // 2. Add to the Heap (Priority Queue)
    requestHeap.add(newReq);

    // 3. Refresh the List on the screen so you can see it
    queueListView.getItems().clear();
    for (EmergencyRequest req : requestHeap) {
        queueListView.getItems().add(req.toString());
    }

            List<String> compatibleTypes = getCompatibleBloodTypes(patientType);

            if (compatibleTypes.isEmpty()) {
                resultsBox.getChildren().add(new Label("Invalid blood type entered. Please use formats like A+, O-, AB+"));
                return;
            }

            ArrayList<Donor> allMatchingDonors = new ArrayList<>();

            for (String compType : compatibleTypes) {
                ArrayList<Donor> resultsForType = donorTree.search(compType);
                if (resultsForType != null) {
                    allMatchingDonors.addAll(resultsForType);
                }
            }

            if (!allMatchingDonors.isEmpty() && !city.isEmpty()) {

                Label loadingLabel = new Label("Locating " + city + "...");
                resultsBox.getChildren().add(loadingLabel);

                double[] cityCoords = getCoordinatesFromCity(city);

                resultsBox.getChildren().clear();

                if (cityCoords != null) {

                    List<DonorResult> sortedResults = new ArrayList<>();

                    for (Donor d : allMatchingDonors) {
                        double distance = calculateDistance(cityCoords[0], cityCoords[1], d.getLat(), d.getLon());
                        sortedResults.add(new DonorResult(d, distance));
                    }

                    sortedResults.sort((a, b) -> Double.compare(a.distance, b.distance));

                    Label summaryLabel = new Label("Found " + sortedResults.size() + " compatible donor(s) near " + city + ":");
                    summaryLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
                    summaryLabel.setStyle("-fx-text-fill: #2c3e50;");
                    resultsBox.getChildren().add(summaryLabel);

                    VBox hospitalCards = new VBox(10);

                    for (DonorResult result : sortedResults) {

                        HBox card = new HBox(15);
                        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 8; -fx-border-color: #bdc3c7; -fx-border-radius: 8;");
                        card.setAlignment(Pos.CENTER_LEFT);

                        VBox textInfo = new VBox(5);

                        Label nameLabel = new Label(result.donor.getName() + " (" + result.donor.getBloodType() + ")");
                        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

                        if (result.donor.getBloodType().equals(patientType)) {
                            nameLabel.setStyle("-fx-text-fill: #27ae60;");
                        } else {
                            nameLabel.setStyle("-fx-text-fill: #2980b9;");
                        }

                        Label distLabel = new Label(String.format("Approx. Distance: %.1f km", result.distance));
                        distLabel.setFont(Font.font("Arial", 12));

                        textInfo.getChildren().addAll(nameLabel, distLabel);

                        Region spacer = new Region();
                        HBox.setHgrow(spacer, Priority.ALWAYS);

                        Button mapBtn = new Button("Google Maps 🗺️");
                        mapBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

                        String googleMapsUrl = String.format(
                                "https://www.google.com/maps/dir/?api=1&origin=%f,%f&destination=%f,%f",
                                cityCoords[0], cityCoords[1], result.donor.getLat(), result.donor.getLon()
                        );

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
                resultsBox.getChildren().add(new Label("No compatible donors found in the system for " + patientType));
            }
        });

      // --- 1. THE LEFT SIDE (Search Form) ---
        VBox leftSide = new VBox(20);
        leftSide.setAlignment(Pos.TOP_CENTER);
        leftSide.setPadding(new Insets(20));
        // We put all your original UI elements here
        leftSide.getChildren().addAll(titleLabel, bloodField, cityField, searchButton, donorButton, resultsBox);
        leftSide.setMinWidth(450);

        // --- 2. THE RIGHT SIDE (Priority Queue / Heap) ---
        VBox rightSide = new VBox(20);
        rightSide.setAlignment(Pos.TOP_CENTER);
        rightSide.setPadding(new Insets(20));
        // This adds the Heap UI we created earlier
        rightSide.getChildren().add(createPriorityQueueUI()); 
        rightSide.setMinWidth(350);

        // --- 3. THE MAIN BOX (Horizontal Layout) ---
        HBox sideBySideLayout = new HBox(30); 
        sideBySideLayout.setPadding(new Insets(20));
        sideBySideLayout.setAlignment(Pos.CENTER);
        // This "snaps" the left side and right side together
        sideBySideLayout.getChildren().addAll(leftSide, rightSide);

        // --- 4. SHOW THE WINDOW ---
        Scene scene = new Scene(sideBySideLayout, 1000, 750); // Made it wider for the two columns
        stage.setTitle("Emergency Blood Bank - Priority Tracking");
        stage.setScene(scene);
        stage.show();
    }

    private List<String> getCompatibleBloodTypes(String patientType) {
        switch (patientType) {
            case "O-": return Arrays.asList("O-");
            case "O+": return Arrays.asList("O+", "O-");
            case "A-": return Arrays.asList("A-", "O-");
            case "A+": return Arrays.asList("A+", "A-", "O+", "O-");
            case "B-": return Arrays.asList("B-", "O-");
            case "B+": return Arrays.asList("B+", "B-", "O+", "O-");
            case "AB-": return Arrays.asList("AB-", "A-", "B-", "O-");
            case "AB+": return Arrays.asList("AB+", "AB-", "A+", "A-", "B+", "B-", "O+", "O-");
            default: return new ArrayList<>();
        }
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

    private static class DonorResult {
        Donor donor;
        double distance;

        public DonorResult(Donor donor, double distance) {
            this.donor = donor;
            this.distance = distance;
        }
    }
    private VBox createPriorityQueueUI() {
    VBox container = new VBox(12);
    container.setPadding(new Insets(15));
    container.setPrefWidth(350);
    container.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d1d8e0; " +
                       "-fx-border-radius: 10; -fx-background-radius: 10; " +
                       "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 4);");

    // Title with Icon
    Label header = new Label("🚑 PRIORITY EMERGENCY QUEUE");
    header.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #c0392b;");

    // Status Label to show Heap size
    Label statusLabel = new Label("Pending Requests: 0");
    statusLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");

    // The ListView with a CellFactory for colors
    queueListView.setPrefHeight(300);
    queueListView.setCellFactory(lv -> new ListCell<String>() {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setStyle("");
            } else {
                setText(item);
                if (item.contains("CRITICAL")) {
                    setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-background-color: #fff5f5;");
                } else if (item.contains("URGENT")) {
                    setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: #2c3e50;");
                }
            }
        }
        
    });
    // This part tells the list how to "paint" the rows
queueListView.setCellFactory(lv -> new ListCell<String>() {
    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setStyle(""); // Clear style for empty rows
        } else {
            setText(item);
            // If the text contains "CRITICAL", make it RED
            if (item.contains("CRITICAL")) {
                setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-background-color: #fff5f5;");
            } 
            // If it contains "URGENT", make it ORANGE
            else if (item.contains("URGENT")) {
                setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
            } 
            // Normal ones are dark blue/gray
            else {
                setStyle("-fx-text-fill: #2c3e50;");
            }
        }
    }
});

    // The Process Button (The Heap 'Poll' Action)
    Button processBtn = new Button("PROCESS NEXT EMERGENCY");
    processBtn.setMaxWidth(Double.MAX_VALUE);
    processBtn.setPadding(new Insets(10));
    processBtn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");

    processBtn.setOnAction(e -> {
        if (!requestHeap.isEmpty()) {
            requestHeap.poll(); // Binary Heap Extract-Max Operation
            updateQueueDisplay(statusLabel);
        }
    });

    container.getChildren().addAll(header, statusLabel, queueListView, processBtn);
    return container;
}

// Helper method to refresh the UI
private void updateQueueDisplay(Label status) {
    queueListView.getItems().clear();
    for (EmergencyRequest req : requestHeap) {
        queueListView.getItems().add(req.toString());
    }
    status.setText("Pending Requests: " + requestHeap.size());
}

}
