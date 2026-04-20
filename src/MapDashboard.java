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
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class MapDashboard extends Application {

    // --- ARCHITECTURE ---
    private RedBlackTree donorTree; 
    private HashMap<String, ArrayList<Volunteer>> volunteerDatabase; 
    private HashMap<String, double[]> cityCoordinates; 
    
    // --- EMERGENCY HEAP ---
    private PriorityQueue<EmergencyRequest> requestHeap;
    private ListView<String> queueListView; 

    private final DropShadow cardShadow = new DropShadow(15, Color.rgb(0, 0, 0, 0.08));

    @Override
    public void start(Stage stage) {
        donorTree = new RedBlackTree();
        volunteerDatabase = new HashMap<>();
        requestHeap = new PriorityQueue<>();
        queueListView = new ListView<>();
        initializeCityCoordinates();

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: #f4f6f9; -fx-font-family: 'Segoe UI', Arial, sans-serif;");

        Tab patientTab = new Tab("🩺 Search & Register");
        patientTab.setContent(createPatientDashboard(stage));
        
        Tab queueTab = new Tab("🚑 Emergency Dispatch");
        queueTab.setContent(createEmergencyQueueUI());

        Tab adminTab = new Tab("🔒 Admin Portal");
        adminTab.setContent(createAdminPortal(stage));

        tabPane.getTabs().addAll(patientTab, queueTab, adminTab);

        Scene scene = new Scene(tabPane, 750, 800);
        stage.setTitle("Emergency Blood Bank System - Master Branch");
        stage.setScene(scene);
        stage.show();
    }

    // ==========================================
    // UI BUILDER: PATIENT DASHBOARD
    // ==========================================
    private VBox createPatientDashboard(Stage stage) {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setPadding(new Insets(30));
        layout.setStyle("-fx-background-color: #f4f6f9;");

        Label titleLabel = new Label("Find Compatible Blood Fast");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        
        VBox searchCard = new VBox(15);
        searchCard.setStyle("-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 12;");
        searchCard.setEffect(cardShadow);
        searchCard.setAlignment(Pos.CENTER);

        TextField bloodField = new TextField();
        bloodField.setPromptText("Patient Blood Type (e.g., A+)");
        styleInputField(bloodField);

        TextField cityField = new TextField();
        cityField.setPromptText("Patient Location (e.g., Colombo)");
        styleInputField(cityField);
        
        Button searchButton = new Button("🔍 SEARCH & REPORT EMERGENCY");
        stylePrimaryButton(searchButton, "#e74c3c", "#c0392b");

        Button registerBtn = new Button("🩸 BECOME A DONOR");
        stylePrimaryButton(registerBtn, "#27ae60", "#2ecc71");
        registerBtn.setOnAction(e -> openVolunteerRegistrationPopup(stage));

        HBox btnRow = new HBox(15, searchButton, registerBtn);
        btnRow.setAlignment(Pos.CENTER);

        searchCard.getChildren().addAll(bloodField, cityField, btnRow);

        VBox resultsBox = new VBox(15);
        resultsBox.setAlignment(Pos.TOP_CENTER);

        searchButton.setOnAction(e -> {
            resultsBox.getChildren().clear(); 
            String patientType = bloodField.getText().toUpperCase().trim();
            String city = cityField.getText().trim();

            if (patientType.isEmpty() || city.isEmpty()) {
                resultsBox.getChildren().add(createAlertMessage("❌ Please enter both Blood Type and City."));
                return;
            }

            int priority = (patientType.equals("O-") || patientType.equals("AB-")) ? 3 : 2;
            requestHeap.add(new EmergencyRequest("Patient in " + city, patientType, priority));
            updateQueueDisplay();

            List<String> compatibleTypes = getCompatibleBloodTypes(patientType);
            double[] cityCoords = getCoordinatesFromCity(city);
            
            if (cityCoords == null) {
                resultsBox.getChildren().add(createAlertMessage("❌ Could not locate your city. Check spelling."));
                return;
            }

            // --- PHASE 1: SEARCH HOSPITALS ---
            List<HospitalResult> hospitalResults = new ArrayList<>();
            double MAX_DISTANCE_KM = 50.0;

            for (String compType : compatibleTypes) {
                ArrayList<Donor> resultsForType = donorTree.search(compType);
                if (resultsForType != null) {
                    for(Donor d : resultsForType) {
                        if(d.supply > 0) {
                            double dist = calculateDistance(cityCoords[0], cityCoords[1], d.lat, d.lon);
                            if (dist <= MAX_DISTANCE_KM) {
                                hospitalResults.add(new HospitalResult(d, dist));
                            }
                        }
                    }
                }
            }

            if (!hospitalResults.isEmpty()) {
                hospitalResults.sort((a, b) -> Double.compare(a.distance, b.distance));
                resultsBox.getChildren().add(new Label("🏥 Hospitals with active supply within 50km:"));
                
                VBox cards = new VBox(10);
                for (HospitalResult result : hospitalResults) {
                    HBox card = new HBox(15);
                    card.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 10;");
                    card.setEffect(new DropShadow(10, Color.rgb(0,0,0,0.05)));
                    card.setAlignment(Pos.CENTER_LEFT);

                    VBox textInfo = new VBox(8);
                    Label nameLabel = new Label(result.donor.name + " (" + result.donor.bloodType + ")");
                    nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
                    nameLabel.setStyle(result.donor.bloodType.equals(patientType) ? "-fx-text-fill: #27ae60;" : "-fx-text-fill: #2980b9;"); 

                    Label stockLabel = new Label("Supply: " + result.donor.supply + " Units  |  Demand: " + result.donor.demand + " Units");
                    stockLabel.setFont(Font.font("Segoe UI", 13));
                    stockLabel.setStyle(result.donor.supply < result.donor.demand ? "-fx-text-fill: #c0392b; -fx-font-weight: bold;" : "-fx-text-fill: #7f8c8d;");

                    Label distLabel = new Label(String.format("📍 Approx. Distance: %.1f km", result.distance));
                    distLabel.setFont(Font.font("Segoe UI", 13));
                    distLabel.setStyle("-fx-text-fill: #7f8c8d;");
                    
                    textInfo.getChildren().addAll(nameLabel, stockLabel, distLabel);
                    Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
                    Button mapBtn = new Button("Map Route 🗺️");
                    stylePrimaryButton(mapBtn, "#3498db", "#2980b9");
                    String mapsUrl = String.format("https://www.google.com/maps/dir/%f,%f/%f,%f", cityCoords[0], cityCoords[1], result.donor.lat, result.donor.lon);
                    mapBtn.setOnAction(ev -> getHostServices().showDocument(mapsUrl));

                    card.getChildren().addAll(textInfo, spacer, mapBtn);
                    cards.getChildren().add(card);
                }
                ScrollPane scroll = new ScrollPane(cards);
                scroll.setPrefHeight(350); scroll.getStyleClass().add("edge-to-edge");
                resultsBox.getChildren().add(scroll);

            } else {
                // --- PHASE 2: VOLUNTEER FALLBACK (REDESIGNED) ---
                Label warning = new Label("⚠️ Hospital supplies depleted or too far! Initiating Volunteer Search...");
                warning.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold; -fx-background-color: #fdebd0; -fx-padding: 10; -fx-background-radius: 5;");
                resultsBox.getChildren().add(warning);

                List<VolunteerResult> volResults = new ArrayList<>();
                for (String compType : compatibleTypes) {
                    ArrayList<Volunteer> volunteers = volunteerDatabase.get(compType);
                    if (volunteers != null) {
                        for(Volunteer v : volunteers) {
                            double dist = calculateDistance(cityCoords[0], cityCoords[1], v.lat, v.lon);
                            volResults.add(new VolunteerResult(v, dist));
                        }
                    }
                }

                if(!volResults.isEmpty()) {
                    volResults.sort((a, b) -> Double.compare(a.distance, b.distance));
                    VBox cards = new VBox(10);
                    for(VolunteerResult result : volResults) {
                        HBox card = new HBox(15);
                        card.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 10; -fx-border-color: #f39c12; -fx-border-width: 2;");
                        card.setEffect(new DropShadow(10, Color.rgb(0,0,0,0.05)));
                        card.setAlignment(Pos.CENTER_LEFT);

                        VBox textInfo = new VBox(5);
                        Label nameLabel = new Label(result.volunteer.name + " (" + result.volunteer.bloodType + ")");
                        nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
                        Label ageLabel = new Label("Age: " + result.volunteer.age + "  |  📍 " + String.format("%.1f km away", result.distance));
                        ageLabel.setStyle("-fx-text-fill: #7f8c8d;");
                        textInfo.getChildren().addAll(nameLabel, ageLabel);

                        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

                        Button viewBtn = new Button("View Info ℹ️");
                        stylePrimaryButton(viewBtn, "#f39c12", "#e67e22");
                        viewBtn.setOnAction(ev -> showVolunteerDetailsPopup(result.volunteer));

                        Button callBtn = new Button("📞 Call");
                        stylePrimaryButton(callBtn, "#27ae60", "#2ecc71");
                        callBtn.setOnAction(ev -> getHostServices().showDocument("tel:" + result.volunteer.phone));

                        HBox actions = new HBox(10, viewBtn, callBtn);
                        card.getChildren().addAll(textInfo, spacer, actions);
                        cards.getChildren().add(card);
                    }
                    ScrollPane scroll = new ScrollPane(cards);
                    scroll.setPrefHeight(350); scroll.getStyleClass().add("edge-to-edge");
                    resultsBox.getChildren().add(scroll);
                } else {
                    resultsBox.getChildren().add(createAlertMessage("❌ No hospitals or volunteers found nearby."));
                }
            }
        });

        layout.getChildren().addAll(titleLabel, searchCard, resultsBox);
        return layout;
    }

    private void showVolunteerDetailsPopup(Volunteer v) {
        Stage detailStage = new Stage();
        detailStage.setTitle("Donor Medical Profile");
        VBox layout = new VBox(15); layout.setPadding(new Insets(20)); layout.setStyle("-fx-background-color: white;");
        Label nameHeader = new Label(v.name + "'s Profile"); nameHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Blood Type:"), 0, 0); grid.add(new Label(v.bloodType), 1, 0);
        grid.add(new Label("Age:"), 0, 1); grid.add(new Label(v.age + " yrs"), 1, 1);
        grid.add(new Label("Weight:"), 0, 2); grid.add(new Label(v.weight + " kg"), 1, 2);
        grid.add(new Label("Hemoglobin:"), 0, 3); grid.add(new Label(v.hemoglobin + " g/dL"), 1, 3);
        grid.add(new Label("Pregnancy:"), 0, 4); grid.add(new Label(v.isPregnant ? "Yes" : "No"), 1, 4);
        Button closeBtn = new Button("Close"); stylePrimaryButton(closeBtn, "#7f8c8d", "#95a5a6");
        closeBtn.setOnAction(e -> detailStage.close());
        layout.getChildren().addAll(nameHeader, new Separator(), grid, closeBtn);
        detailStage.setScene(new Scene(layout, 300, 350)); detailStage.show();
    }

    private void openVolunteerRegistrationPopup(Stage parentStage) {
        Stage popupStage = new Stage();
        popupStage.initOwner(parentStage);
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.setTitle("Volunteer Donor Registration");

        VBox layout = new VBox(12);
        layout.setPadding(new Insets(25));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: white;");

        TextField nameField = new TextField(); nameField.setPromptText("Full Name");
        TextField phoneField = new TextField(); phoneField.setPromptText("Phone Number");
        ComboBox<String> cityCombo = new ComboBox<>(); cityCombo.getItems().addAll(cityCoordinates.keySet()); cityCombo.setPromptText("Your City");
        ComboBox<String> bloodCombo = new ComboBox<>(); bloodCombo.getItems().addAll("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"); bloodCombo.setPromptText("Blood Type");
        TextField ageField = new TextField(); ageField.setPromptText("Age");
        TextField weightField = new TextField(); weightField.setPromptText("Weight (kg)");
        TextField hbField = new TextField(); hbField.setPromptText("Hemoglobin level");
        CheckBox pregnantBox = new CheckBox("Currently Pregnant");

        Button submitBtn = new Button("Register & Save Lives");
        stylePrimaryButton(submitBtn, "#27ae60", "#2ecc71");
        Label status = new Label();

        submitBtn.setOnAction(e -> {
            try {
                int age = Integer.parseInt(ageField.getText().trim());
                double weight = Double.parseDouble(weightField.getText().trim());
                double hb = Double.parseDouble(hbField.getText().trim());
                if (age < 18 || age > 60 || weight < 50 || hb < 12 || pregnantBox.isSelected()) {
                    status.setText("❌ Does not meet medical criteria."); return;
                }
                double[] coords = getCoordinatesFromCity(cityCombo.getValue());
                Volunteer v = new Volunteer(nameField.getText(), bloodCombo.getValue(), phoneField.getText(), coords[0], coords[1], age, weight, hb, pregnantBox.isSelected());
                volunteerDatabase.putIfAbsent(v.bloodType, new ArrayList<>());
                volunteerDatabase.get(v.bloodType).add(v);
                saveVolunteerToFile(v);
                status.setText("✅ Medically Cleared & Registered!");
            } catch (Exception ex) { status.setText("❌ Error: Check inputs."); }
        });

        layout.getChildren().addAll(new Label("Medical Clearance Form"), nameField, phoneField, cityCombo, bloodCombo, ageField, weightField, hbField, pregnantBox, submitBtn, status);
        popupStage.setScene(new Scene(layout, 350, 550)); popupStage.show();
    }

    private VBox createEmergencyQueueUI() {
        VBox container = new VBox(15); container.setPadding(new Insets(30)); container.setAlignment(Pos.TOP_CENTER);
        Label header = new Label("🚑 Priority Emergency Dispatch"); header.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        queueListView.setPrefHeight(500);
        queueListView.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else { setText(item); setStyle(item.contains("CRITICAL") ? "-fx-text-fill: #e74c3c; -fx-font-weight: bold;" : "-fx-text-fill: #2c3e50;"); }
            }
        });
        Button processBtn = new Button("PROCESS NEXT EMERGENCY"); stylePrimaryButton(processBtn, "#c0392b", "#a93226");
        processBtn.setOnAction(e -> { if (!requestHeap.isEmpty()) { requestHeap.poll(); updateQueueDisplay(); } });
        container.getChildren().addAll(header, queueListView, processBtn);
        return container;
    }

    private void updateQueueDisplay() {
        queueListView.getItems().clear();
        for (EmergencyRequest req : requestHeap) queueListView.getItems().add(req.toString());
    }

    private VBox createAdminPortal(Stage stage) {
        VBox layout = new VBox(30); layout.setAlignment(Pos.TOP_CENTER); layout.setPadding(new Insets(30));
        VBox bulkBox = new VBox(15); bulkBox.setStyle("-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 12;"); bulkBox.setEffect(cardShadow); bulkBox.setAlignment(Pos.CENTER);
        Label statusLabel = new Label("Status: Waiting for upload...");
        Button loadDbBtn = new Button("📂 Upload Hospital CSV"); stylePrimaryButton(loadDbBtn, "#f39c12", "#d35400");
        loadDbBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            java.io.File file = fc.showOpenDialog(stage);
            if (file != null) { donorTree = new RedBlackTree(); statusLabel.setText("✅ " + loadDatabaseFromCSV(file.getAbsolutePath()) + " records imported."); }
        });
        bulkBox.getChildren().addAll(new Label("Bulk Import"), loadDbBtn, statusLabel);
        layout.getChildren().addAll(new Label("Admin Portal"), bulkBox);
        return layout;
    }

    private void saveVolunteerToFile(Volunteer v) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("volunteers.txt", true))) {
            bw.write(v.toCSV()); bw.newLine();
        } catch (IOException e) { System.out.println("File Error"); }
    }

    private void styleInputField(TextField f) { f.setStyle("-fx-font-size: 14px; -fx-padding: 10; -fx-background-radius: 8; -fx-border-color: #bdc3c7;"); }

    private void stylePrimaryButton(Button b, String c, String h) {
        String s = "-fx-background-color: " + c + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12 25; -fx-background-radius: 8; -fx-cursor: hand;";
        b.setStyle(s);
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: " + h + "; " + s.substring(29)));
        b.setOnMouseExited(e -> b.setStyle(s));
    }

    private Label createAlertMessage(String t) { Label l = new Label(t); l.setStyle("-fx-text-fill: #c0392b; -fx-background-color: #fadbd8; -fx-padding: 10; -fx-background-radius: 8;"); return l; }

    private int loadDatabaseFromCSV(String p) {
        int count = 0;
        try (Scanner sc = new Scanner(new java.io.File(p))) {
            if (sc.hasNextLine()) sc.nextLine();
            while (sc.hasNextLine()) {
                String[] d = sc.nextLine().split(",");
                double[] c = getCoordinatesFromCity(d[1].trim());
                if (c != null) { donorTree.insert(new Donor(d[0].trim(), d[2].trim(), c[0], c[1], Integer.parseInt(d[3].trim()), Integer.parseInt(d[4].trim()))); count++; }
            }
        } catch (Exception e) { return 0; }
        return count;
    }

    private void initializeCityCoordinates() {
        cityCoordinates = new HashMap<>();
        cityCoordinates.put("Colombo", new double[]{6.918, 79.869});
        cityCoordinates.put("Kandy", new double[]{7.290, 80.633});
        cityCoordinates.put("Galle", new double[]{6.066, 80.225});
        cityCoordinates.put("Jaffna", new double[]{9.661, 80.025});
    }

    private List<String> getCompatibleBloodTypes(String t) {
        switch (t) {
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

    private double calculateDistance(double la1, double lo1, double la2, double lo2) {
        double dLat = Math.toRadians(la2 - la1), dLon = Math.toRadians(lo2 - lo1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(la1)) * Math.cos(Math.toRadians(la2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371 * (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }

    private double[] getCoordinatesFromCity(String n) {
        try {
            for (String k : cityCoordinates.keySet()) if (k.equalsIgnoreCase(n.trim())) return cityCoordinates.get(k);
            String u = "https://nominatim.openstreetmap.org/search?q=" + java.net.URLEncoder.encode(n.trim(), "UTF-8") + "%2CSri+Lanka&format=json&limit=1";
            HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
            c.setRequestProperty("User-Agent", "NIBM_Student_Project/1.0");
            Scanner s = new Scanner(c.getInputStream());
            if (!s.hasNext()) return null;
            String r = s.useDelimiter("\\A").next();
            if (r.contains("\"lat\":\"")) return new double[]{Double.parseDouble(r.split("\"lat\":\"")[1].split("\"")[0]), Double.parseDouble(r.split("\"lon\":\"")[1].split("\"")[0])};
        } catch (Exception e) {} return null;
    }

    public static void main(String[] args) { launch(args); }
    private static class HospitalResult { Donor donor; double distance; public HospitalResult(Donor d, double dist) { this.donor = d; this.distance = dist; } }
    private static class VolunteerResult { Volunteer volunteer; double distance; public VolunteerResult(Volunteer v, double dist) { this.volunteer = v; this.distance = dist; } }
}