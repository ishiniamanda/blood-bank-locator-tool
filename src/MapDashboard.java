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
import java.util.Map;
import java.util.Scanner;

public class MapDashboard extends Application {

    private RedBlackTree donorTree;
    private Graph graph;

    @Override
    public void start(Stage stage) {

        donorTree = new RedBlackTree();
        graph = new Graph();

        // ================= GRAPH =================
        graph.addEdge("Colombo", "Negombo", 40);
        graph.addEdge("Colombo", "Galle", 120);
        graph.addEdge("Negombo", "Kandy", 100);
        graph.addEdge("Kandy", "Jaffna", 250);
        graph.addEdge("Galle", "Matara", 30);

        // ================= DONORS =================
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
        bloodField.setPromptText("Enter Blood Type (e.g. O+)");
        bloodField.setStyle("-fx-font-size: 14px; -fx-padding: 10px;");

        TextField cityField = new TextField();
        cityField.setPromptText("Enter Your City (e.g. Jaffna)");
        cityField.setStyle("-fx-font-size: 14px; -fx-padding: 10px;");

        Button searchButton = new Button("SEARCH DONORS");
        searchButton.setStyle(
                "-fx-background-color: #e74c3c;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 10 20;"
        );

        Button routeButton = new Button("FIND SHORTEST ROUTE");
        routeButton.setStyle(
                "-fx-background-color: #2ecc71;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 10 20;"
        );

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

            ArrayList<Donor> rawList = donorTree.search(type);
            ArrayList<Donor> matchingDonors = new ArrayList<>();

            for (Donor d : rawList) {
                if (BloodFilter.isCompatible(type, d.bloodType)) {
                    matchingDonors.add(d);
                }
            }

            if (!matchingDonors.isEmpty() && !city.isEmpty()) {

                double[] cityCoords = getCoordinatesFromCity(city);
                resultsBox.getChildren().clear();

                if (cityCoords != null) {

                    List<DonorResult> sortedResults = new ArrayList<>();

                    for (Donor d : matchingDonors) {
                        double distance = calculateDistance(cityCoords[0], cityCoords[1], d.lat, d.lon);
                        sortedResults.add(new DonorResult(d, distance));
                    }

                    sortedResults.sort((a, b) -> Double.compare(a.distance, b.distance));

                    Label summaryLabel = new Label(
                            "Found " + sortedResults.size() + " donor(s) near " + city
                    );
                    summaryLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
                    resultsBox.getChildren().add(summaryLabel);

                    VBox hospitalCards = new VBox(10);

                    for (DonorResult result : sortedResults) {

                        HBox card = new HBox(15);
                        card.setStyle("-fx-background-color: white; -fx-padding: 15;");
                        card.setAlignment(Pos.CENTER_LEFT);

                        VBox textInfo = new VBox(5);

                        Label nameLabel = new Label(result.donor.name);
                        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

                        Label distLabel = new Label("Distance: " + String.format("%.2f", result.distance) + " km");

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
                resultsBox.getChildren().add(new Label("No donor found."));
            }
        });

        // ================= DIJKSTRA BUTTON (FIXED ONLY) =================
        routeButton.setOnAction(e -> {

            resultsBox.getChildren().clear();

            String start = cityField.getText().trim();

            // ✅ FIX: case-insensitive graph matching
            String matchedNode = null;

            for (String node : graph.getGraph().keySet()) {
                if (node.equalsIgnoreCase(start)) {
                    matchedNode = node;
                    break;
                }
            }

            if (matchedNode == null) {
                resultsBox.getChildren().add(new Label(
                        "Invalid start location!\nValid: Colombo, Negombo, Galle, Kandy, Jaffna, Matara"
                ));
                return;
            }

            Map<String, Integer> result =
                    Dijkstra.findShortestPaths(graph, matchedNode);

            int minDistance = Integer.MAX_VALUE;

            for (int value : result.values()) {
                if (value < minDistance) {
                    minDistance = value;
                }
            }

            Label title = new Label("Shortest Routes from " + matchedNode);
            title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            resultsBox.getChildren().add(title);

            for (Map.Entry<String, Integer> entry : result.entrySet()) {

                String distText = (entry.getValue() == Integer.MAX_VALUE)
                        ? "No Path"
                        : entry.getValue() + " km";

                Label label = new Label(entry.getKey() + " → " + distText);

                if (entry.getValue() == minDistance && minDistance != Integer.MAX_VALUE) {
                    label.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                }

                resultsBox.getChildren().add(label);
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