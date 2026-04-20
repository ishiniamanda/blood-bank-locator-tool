import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class MapDashboard extends Application {
    
    private RedBlackTree donorTree;
    private HashMap<String, double[]> cityCoordinates;

    // A reusable modern drop shadow for our UI cards
    private final DropShadow cardShadow = new DropShadow(15, Color.rgb(0, 0, 0, 0.08));

    @Override
    public void start(Stage stage) {
        donorTree = new RedBlackTree();
        initializeCityCoordinates();

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: #f4f6f9; -fx-font-family: 'Segoe UI', Arial, sans-serif;");

        Tab patientTab = new Tab(" Patient Dashboard");
        patientTab.setContent(createPatientDashboard(stage));
        patientTab.setStyle("-fx-font-size: 14px; -fx-padding: 5 15;");

        Tab adminTab = new Tab("🔒 Admin Portal");
        adminTab.setContent(createAdminPortal(stage));
        adminTab.setStyle("-fx-font-size: 14px; -fx-padding: 5 15;");

        tabPane.getTabs().addAll(patientTab, adminTab);

        Scene scene = new Scene(tabPane, 700, 800);
        stage.setTitle("Emergency Blood Bank System");
        stage.setScene(scene);
        stage.show();
    }

    // ==========================================
    // UI BUILDER: PATIENT DASHBOARD
    // ==========================================
    private VBox createPatientDashboard(Stage stage) {
        VBox layout = new VBox(25);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setPadding(new Insets(30));
        layout.setStyle("-fx-background-color: #f4f6f9;"); // Light modern gray background

        // --- HERO HEADER ---
        Label titleLabel = new Label("Find Compatible Blood Fast");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        titleLabel.setStyle("-fx-text-fill: #2c3e50;");
        
        Label subtitleLabel = new Label("Search the national database for emergency blood availability.");
        subtitleLabel.setFont(Font.font("Segoe UI", 14));
        subtitleLabel.setStyle("-fx-text-fill: #7f8c8d;");
        
        VBox headerBox = new VBox(5, titleLabel, subtitleLabel);
        headerBox.setAlignment(Pos.CENTER);

        // --- SEARCH CARD ---
        VBox searchCard = new VBox(15);
        searchCard.setStyle("-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 12;");
        searchCard.setEffect(cardShadow);
        searchCard.setAlignment(Pos.CENTER);

        TextField bloodField = new TextField();
        bloodField.setPromptText("Patient Blood Type (e.g., A+)");
        styleInputField(bloodField);

        TextField cityField = new TextField();
        cityField.setPromptText("Your Location (e.g., Colombo)");
        styleInputField(cityField);
        
        Button searchButton = new Button("🔍 SEARCH DONORS");
        stylePrimaryButton(searchButton, "#e74c3c", "#c0392b"); // Red Button

        searchCard.getChildren().addAll(bloodField, cityField, searchButton);

        // --- RESULTS AREA ---
        VBox resultsBox = new VBox(15);
        resultsBox.setAlignment(Pos.TOP_CENTER);
        resultsBox.setPadding(new Insets(10, 0, 0, 0));

        searchButton.setOnAction(e -> {
            resultsBox.getChildren().clear(); 
            String patientType = bloodField.getText().toUpperCase().trim();
            String city = cityField.getText().trim();
            
            if (donorTree.search("O+") == null && donorTree.search("A+") == null) {
                resultsBox.getChildren().add(createAlertMessage("⚠️ Database is empty. Please ask an Admin to load data."));
                return;
            }

            List<String> compatibleTypes = getCompatibleBloodTypes(patientType);
            if (compatibleTypes.isEmpty()) {
                resultsBox.getChildren().add(createAlertMessage("❌ Invalid blood type. Please use A+, O-, AB+, etc."));
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
                Label loadingLabel = new Label("Locating nearby hospitals...");
                loadingLabel.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                resultsBox.getChildren().add(loadingLabel);
                
                double[] cityCoords = getCoordinatesFromCity(city);
                resultsBox.getChildren().clear(); 
                
                if (cityCoords != null) {
                    List<DonorResult> sortedResults = new ArrayList<>();
                    for (Donor d : allMatchingDonors) {
                        double distance = calculateDistance(cityCoords[0], cityCoords[1], d.lat, d.lon);
                        sortedResults.add(new DonorResult(d, distance));
                    }

                    sortedResults.sort((a, b) -> Double.compare(a.distance, b.distance));

                    Label summaryLabel = new Label("Found " + sortedResults.size() + " compatible donor(s) near " + city + ":");
                    summaryLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
                    summaryLabel.setStyle("-fx-text-fill: #2c3e50;");
                    resultsBox.getChildren().add(summaryLabel);

                    VBox hospitalCards = new VBox(12); // Spacing between results
                    hospitalCards.setPadding(new Insets(5, 10, 5, 5));
                    
                    for (DonorResult result : sortedResults) {
                        HBox card = new HBox(15);
                        // Modern Result Card Styling
                        card.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 10; -fx-border-color: transparent;");
                        card.setEffect(new DropShadow(10, Color.rgb(0,0,0,0.05)));
                        card.setAlignment(Pos.CENTER_LEFT);

                        VBox textInfo = new VBox(8);
                        Label nameLabel = new Label(result.donor.name + " (" + result.donor.bloodType + ")");
                        nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
                        nameLabel.setStyle(result.donor.bloodType.equals(patientType) ? "-fx-text-fill: #27ae60;" : "-fx-text-fill: #2980b9;"); 
                        
                        Label stockLabel = new Label("Supply: " + result.donor.supply + " Units  |  Demand: " + result.donor.demand + " Units");
                        stockLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
                        stockLabel.setStyle(result.donor.supply < result.donor.demand ? "-fx-text-fill: #e74c3c; -fx-background-color: #fadbd8; -fx-padding: 3 8; -fx-background-radius: 5;" : "-fx-text-fill: #27ae60; -fx-background-color: #d5f5e3; -fx-padding: 3 8; -fx-background-radius: 5;");

                        Label distLabel = new Label(String.format("📍 Approx. Distance: %.1f km", result.distance));
                        distLabel.setFont(Font.font("Segoe UI", 13));
                        distLabel.setStyle("-fx-text-fill: #7f8c8d;");
                        
                        textInfo.getChildren().addAll(nameLabel, stockLabel, distLabel);

                        Region spacer = new Region();
                        HBox.setHgrow(spacer, Priority.ALWAYS);

                        Button mapBtn = new Button("Map Route 🗺️");
                        stylePrimaryButton(mapBtn, "#3498db", "#2980b9"); // Blue Map Button
                        String googleMapsUrl = String.format("https://www.google.com/maps/dir/?api=1&origin=%f,%f&destination=%f,%f", cityCoords[0], cityCoords[1], result.donor.lat, result.donor.lon);
                        mapBtn.setOnAction(ev -> getHostServices().showDocument(googleMapsUrl));

                        card.getChildren().addAll(textInfo, spacer, mapBtn);
                        hospitalCards.getChildren().add(card);
                    }

                    ScrollPane scrollPane = new ScrollPane(hospitalCards);
                    scrollPane.setFitToWidth(true);
                    scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
                    scrollPane.setPrefHeight(400);
                    scrollPane.getStyleClass().add("edge-to-edge"); // Removes default ugly border
                    resultsBox.getChildren().add(scrollPane);
                } else {
                    resultsBox.getChildren().add(createAlertMessage("❌ Error: Could not locate your city."));
                }
            } else {
                resultsBox.getChildren().add(createAlertMessage("🔍 No compatible donors found in the system."));
            }
        });

        layout.getChildren().addAll(headerBox, searchCard, resultsBox);
        return layout;
    }

    // ==========================================
    // UI BUILDER: ADMIN PORTAL
    // ==========================================
    private VBox createAdminPortal(Stage stage) {
        VBox layout = new VBox(30);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setPadding(new Insets(30));
        layout.setStyle("-fx-background-color: #f4f6f9;");

        Label titleLabel = new Label("Database Management");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        titleLabel.setStyle("-fx-text-fill: #2c3e50;");

        // --- BULK UPLOAD CARD ---
        VBox bulkBox = new VBox(15);
        bulkBox.setStyle("-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 12;");
        bulkBox.setEffect(cardShadow);
        bulkBox.setAlignment(Pos.CENTER);
        
        Label bulkTitle = new Label("Bulk Import (CSV Data)");
        bulkTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        bulkTitle.setStyle("-fx-text-fill: #2c3e50;");
        
        Label systemStatusLabel = new Label("Status: Waiting for data upload...");
        systemStatusLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");

        Button loadDbBtn = new Button("📂 Upload CSV Database");
        stylePrimaryButton(loadDbBtn, "#f39c12", "#d35400"); // Orange Admin Button

        loadDbBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Blood Bank CSV");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            java.io.File selectedFile = fileChooser.showOpenDialog(stage);
            
            if (selectedFile != null) {
                donorTree = new RedBlackTree(); 
                int loadedCount = loadDatabaseFromCSV(selectedFile.getAbsolutePath());
                if (loadedCount > 0) {
                    systemStatusLabel.setText("✅ Status: " + loadedCount + " records imported successfully.");
                    systemStatusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-background-color: #d5f5e3; -fx-padding: 5 10; -fx-background-radius: 5;");
                } else {
                    systemStatusLabel.setText("❌ Status: Error loading file.");
                    systemStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                }
            }
        });
        bulkBox.getChildren().addAll(bulkTitle, loadDbBtn, systemStatusLabel);

        // --- MANUAL ENTRY CARD ---
        VBox manualBox = new VBox(15);
        manualBox.setStyle("-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 12;");
        manualBox.setEffect(cardShadow);
        manualBox.setAlignment(Pos.CENTER);

        Label manualTitle = new Label("Manual Hospital Entry");
        manualTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        manualTitle.setStyle("-fx-text-fill: #2c3e50;");

        TextField nameInput = new TextField();
        nameInput.setPromptText("Hospital Name");
        styleInputField(nameInput);
        
        ComboBox<String> cityCombo = new ComboBox<>();
        cityCombo.getItems().addAll(cityCoordinates.keySet());
        cityCombo.setPromptText("Select City");
        cityCombo.setStyle("-fx-font-size: 14px; -fx-background-radius: 8; -fx-padding: 5;");
        
        ComboBox<String> bloodCombo = new ComboBox<>();
        bloodCombo.getItems().addAll("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
        bloodCombo.setPromptText("Blood Type");
        bloodCombo.setStyle("-fx-font-size: 14px; -fx-background-radius: 8; -fx-padding: 5;");

        HBox combos = new HBox(15, cityCombo, bloodCombo);
        combos.setAlignment(Pos.CENTER);

        TextField supplyInput = new TextField();
        supplyInput.setPromptText("Supply (Units)");
        styleInputField(supplyInput);
        
        TextField demandInput = new TextField();
        demandInput.setPromptText("Demand (Units)");
        styleInputField(demandInput);
        
        HBox numbers = new HBox(15, supplyInput, demandInput);
        numbers.setAlignment(Pos.CENTER);

        Label manualStatus = new Label();
        manualStatus.setStyle("-fx-font-weight: bold;");

        Button addManualBtn = new Button("➕ Add Record to Database");
        stylePrimaryButton(addManualBtn, "#27ae60", "#2ecc71"); // Green Add Button

        addManualBtn.setOnAction(e -> {
            try {
                String name = nameInput.getText().trim();
                String city = cityCombo.getValue();
                String type = bloodCombo.getValue();
                int sup = Integer.parseInt(supplyInput.getText().trim());
                int dem = Integer.parseInt(demandInput.getText().trim());

                if (name.isEmpty() || city == null || type == null) throw new Exception();

                double[] coords = cityCoordinates.get(city);
                donorTree.insert(new Donor(name, type, coords[0], coords[1], sup, dem));
                
                manualStatus.setText("✅ Successfully added " + name);
                manualStatus.setStyle("-fx-text-fill: #27ae60; -fx-background-color: #d5f5e3; -fx-padding: 5 10; -fx-background-radius: 5;");
                
                nameInput.clear(); supplyInput.clear(); demandInput.clear();
                cityCombo.setValue(null); bloodCombo.setValue(null);
            } catch (Exception ex) {
                manualStatus.setText("❌ Error: Please fill all fields correctly.");
                manualStatus.setStyle("-fx-text-fill: #e74c3c;");
            }
        });

        manualBox.getChildren().addAll(manualTitle, nameInput, combos, numbers, addManualBtn, manualStatus);

        layout.getChildren().addAll(titleLabel, bulkBox, manualBox);
        return layout;
    }

    // ==========================================
    // UI HELPER METHODS (For Modern Styling)
    // ==========================================
    private void styleInputField(TextField field) {
        field.setStyle("-fx-font-size: 14px; -fx-padding: 12 15; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #bdc3c7; -fx-background-color: #f8f9fa;");
        // Focus effect
        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                field.setStyle("-fx-font-size: 14px; -fx-padding: 12 15; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #3498db; -fx-background-color: #ffffff;");
            } else {
                field.setStyle("-fx-font-size: 14px; -fx-padding: 12 15; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #bdc3c7; -fx-background-color: #f8f9fa;");
            }
        });
    }

    private void stylePrimaryButton(Button btn, String defaultColor, String hoverColor) {
        String baseStyle = "-fx-background-color: " + defaultColor + "; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 12 25; -fx-background-radius: 8; -fx-cursor: hand;";
        String hoverStyle = "-fx-background-color: " + hoverColor + "; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 12 25; -fx-background-radius: 8; -fx-cursor: hand;";
        
        btn.setStyle(baseStyle);
        // Add hover animations
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(baseStyle));
    }

    private Label createAlertMessage(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        label.setStyle("-fx-text-fill: #c0392b; -fx-background-color: #fadbd8; -fx-padding: 10 20; -fx-background-radius: 8;");
        return label;
    }

    // ==========================================
    // BACKEND LOGIC & DATA HANDLING (Unchanged)
    // ==========================================
    private int loadDatabaseFromCSV(String filePath) {
        int count = 0; 
        try {
            java.io.File file = new java.io.File(filePath);
            Scanner fileScanner = new Scanner(file);
            if (fileScanner.hasNextLine()) fileScanner.nextLine(); 

            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine();
                String[] data = line.split(","); 
                if (data.length == 5) {
                    String name = data[0].trim();
                    String city = data[1].trim();
                    String bloodType = data[2].trim();
                    int supply = Integer.parseInt(data[3].trim());
                    int demand = Integer.parseInt(data[4].trim());

                    double[] coords = cityCoordinates.get(city);
                    if (coords != null) {
                        donorTree.insert(new Donor(name, bloodType, coords[0], coords[1], supply, demand));
                        count++;
                    }
                }
            }
            fileScanner.close();
            return count;
        } catch (Exception e) {
            return 0;
        }
    }

    private void initializeCityCoordinates() {
        cityCoordinates = new HashMap<>();
        cityCoordinates.put("Colombo", new double[]{6.918, 79.869});
        cityCoordinates.put("Kalubowila", new double[]{6.865, 79.877});
        cityCoordinates.put("Ragama", new double[]{7.029, 79.914});
        cityCoordinates.put("Negombo", new double[]{7.209, 79.848});
        cityCoordinates.put("Gampaha", new double[]{7.087, 79.998});
        cityCoordinates.put("Kalutara", new double[]{6.577, 79.960});
        cityCoordinates.put("Galle", new double[]{6.066, 80.225});
        cityCoordinates.put("Matara", new double[]{5.949, 80.540});
        cityCoordinates.put("Hambantota", new double[]{6.136, 81.118});
        cityCoordinates.put("Kandy", new double[]{7.290, 80.633});
        cityCoordinates.put("Peradeniya", new double[]{7.268, 80.593});
        cityCoordinates.put("Matale", new double[]{7.472, 80.623});
        cityCoordinates.put("Nuwara Eliya", new double[]{6.968, 80.767});
        cityCoordinates.put("Kurunegala", new double[]{7.481, 80.360});
        cityCoordinates.put("Kuliyapitiya", new double[]{7.468, 80.040});
        cityCoordinates.put("Puttalam", new double[]{8.031, 79.833});
        cityCoordinates.put("Chilaw", new double[]{7.576, 79.799});
        cityCoordinates.put("Anuradhapura", new double[]{8.345, 80.410});
        cityCoordinates.put("Polonnaruwa", new double[]{7.935, 81.000});
        cityCoordinates.put("Jaffna", new double[]{9.661, 80.025});
        cityCoordinates.put("Point Pedro", new double[]{9.824, 80.235});
        cityCoordinates.put("Vavuniya", new double[]{8.751, 80.497});
        cityCoordinates.put("Mannar", new double[]{8.980, 79.905});
        cityCoordinates.put("Trincomalee", new double[]{8.571, 81.233});
        cityCoordinates.put("Batticaloa", new double[]{7.710, 81.692});
        cityCoordinates.put("Ampara", new double[]{7.283, 81.674});
        cityCoordinates.put("Badulla", new double[]{6.993, 81.055});
        cityCoordinates.put("Diyatalawa", new double[]{6.816, 80.954});
        cityCoordinates.put("Monaragala", new double[]{6.871, 81.348});
        cityCoordinates.put("Ratnapura", new double[]{6.682, 80.399});
        cityCoordinates.put("Kegalle", new double[]{7.251, 80.345});
    }

    private List<String> getCompatibleBloodTypes(String patientType) {
        switch (patientType) {
            case "O-":  return Arrays.asList("O-"); 
            case "O+":  return Arrays.asList("O+", "O-");
            case "A-":  return Arrays.asList("A-", "O-");
            case "A+":  return Arrays.asList("A+", "A-", "O+", "O-");
            case "B-":  return Arrays.asList("B-", "O-");
            case "B+":  return Arrays.asList("B+", "B-", "O+", "O-");
            case "AB-": return Arrays.asList("AB-", "A-", "B-", "O-");
            case "AB+": return Arrays.asList("AB+", "AB-", "A+", "A-", "B+", "B-", "O+", "O-"); 
            default:    return new ArrayList<>(); 
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
}