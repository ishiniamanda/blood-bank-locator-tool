import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
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
import javafx.util.Duration;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class MapDashboard extends Application {

    private RedBlackTree donorTree; 
    private HashMap<String, ArrayList<Volunteer>> volunteerDatabase; 
    private HashMap<String, double[]> cityCoordinates; 
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
        stage.setTitle("Emergency Blood Bank System - Live GPS Version");
        stage.setScene(scene);
        stage.show();
    }

    // ==========================================
    // 1. PATIENT DASHBOARD (SEARCH & LIVE GPS ROUTING)
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

        TextField bloodField = new TextField(); bloodField.setPromptText("Blood Type (e.g., A+)");
        TextField cityField = new TextField(); cityField.setPromptText("Enter City (for list distance)");
        styleInputField(bloodField); styleInputField(cityField);
        
        Button searchButton = new Button("🔍 SEARCH EMERGENCY");
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
            String userCity = cityField.getText().trim();
            if (patientType.isEmpty() || userCity.isEmpty()) return;

            requestHeap.add(new EmergencyRequest("Patient in " + userCity, patientType, (patientType.equals("O-") || patientType.equals("AB-")) ? 3 : 2));
            updateQueueDisplay();

            double[] cityCoords = getCoordinatesFromCity(userCity);
            if (cityCoords == null) {
                resultsBox.getChildren().add(createAlertMessage("❌ Location not found."));
                return;
            }

            List<HospitalResult> hospitalResults = new ArrayList<>();
            for (String compType : getCompatibleBloodTypes(patientType)) {
                ArrayList<Donor> results = donorTree.search(compType);
                if (results != null) {
                    for(Donor d : results) {
                        double dist = calculateDistance(cityCoords[0], cityCoords[1], d.lat, d.lon);
                        if (dist <= 50.0 && d.supply > 0) hospitalResults.add(new HospitalResult(d, dist));
                    }
                }
            }

            if (!hospitalResults.isEmpty()) {
                hospitalResults.sort((a, b) -> Double.compare(a.distance, b.distance));
                VBox cards = new VBox(10);
                for (HospitalResult res : hospitalResults) {
                    HBox card = new HBox(15);
                    card.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 10;");
                    card.setEffect(new DropShadow(10, Color.rgb(0,0,0,0.05)));
                    
                    VBox info = new VBox(5);
                    Label name = new Label(res.donor.name + " (" + res.donor.bloodType + ")");
                    name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
                    name.setStyle(res.donor.bloodType.equals(patientType) ? "-fx-text-fill: #27ae60;" : "-fx-text-fill: #2980b9;");
                    info.getChildren().addAll(name, new Label("Supply: " + res.donor.supply), new Label(String.format("📍 Approx. %.1f km", res.distance)));
                    
                    Region s = new Region(); HBox.setHgrow(s, Priority.ALWAYS);
                    
                    // --- LIVE GPS ROUTING FIX ---
                    Button map = new Button("View Route 🗺️"); 
                    stylePrimaryButton(map, "#3498db", "#2980b9");
                    map.setOnAction(ev -> {
                        String destination = res.donor.lat + "," + res.donor.lon;
                        // Using the official directions API with no origin forces the browser 
                        // to use the user's real-time GPS location.
                        String url = "https://www.google.com/maps/dir/?api=1&destination=" 
                                   + destination + "&travelmode=driving";
                        getHostServices().showDocument(url);
                    });

                    card.getChildren().addAll(info, s, map); cards.getChildren().add(card);
                }
                ScrollPane scroll = new ScrollPane(cards);
                scroll.setPrefHeight(350); scroll.getStyleClass().add("edge-to-edge");
                resultsBox.getChildren().add(scroll);
            } else {
                // FALLBACK TO VOLUNTEERS
                Label warning = new Label("⚠️ No nearby hospitals. Searching Volunteers...");
                warning.setStyle("-fx-text-fill: #e67e22; -fx-background-color: #fdebd0; -fx-padding: 10; -fx-background-radius: 5;");
                resultsBox.getChildren().add(warning);
                List<VolunteerResult> volRes = new ArrayList<>();
                for (String comp : getCompatibleBloodTypes(patientType)) {
                    ArrayList<Volunteer> vols = volunteerDatabase.get(comp);
                    if (vols != null) for(Volunteer v : vols) volRes.add(new VolunteerResult(v, calculateDistance(cityCoords[0], cityCoords[1], v.lat, v.lon)));
                }
                if(!volRes.isEmpty()) {
                    volRes.sort((a,b) -> Double.compare(a.distance, b.distance));
                    VBox vCards = new VBox(10);
                    for(VolunteerResult vr : volRes) {
                        HBox vCard = new HBox(15); vCard.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-border-color: #f39c12; -fx-border-radius: 10; -fx-border-width: 2;");
                        VBox vInfo = new VBox(5); vInfo.getChildren().addAll(new Label(vr.volunteer.name + " ("+vr.volunteer.bloodType+")"), new Label("Age: " + vr.volunteer.age + " | " + String.format("%.1f km", vr.distance)));
                        Region vs = new Region(); HBox.setHgrow(vs, Priority.ALWAYS);
                        Button vBtn = new Button("View Info"); stylePrimaryButton(vBtn, "#f39c12", "#e67e22");
                        vBtn.setOnAction(ev -> showVolunteerDetailsPopup(vr.volunteer));
                        Button cBtn = new Button("📞 Call"); stylePrimaryButton(cBtn, "#27ae60", "#2ecc71");
                        cBtn.setOnAction(ev -> getHostServices().showDocument("tel:" + vr.volunteer.phone));
                        vCard.getChildren().addAll(vInfo, vs, new HBox(10, vBtn, cBtn)); vCards.getChildren().add(vCard);
                    }
                    resultsBox.getChildren().add(new ScrollPane(vCards));
                }
            }
        });
        layout.getChildren().addAll(titleLabel, searchCard, resultsBox);
        return layout;
    }

    // ==========================================
    // 2. ADMIN PORTAL (ADVANCED DASHBOARD)
    // ==========================================
    private VBox createAdminPortal(Stage stage) {
        VBox mainLayout = new VBox(25);
        mainLayout.setAlignment(Pos.TOP_CENTER);
        mainLayout.setPadding(new Insets(30));
        mainLayout.setStyle("-fx-background-color: #f4f6f9;");

        HBox contentArea = new HBox(30); contentArea.setAlignment(Pos.TOP_CENTER);

        VBox manualEntryCard = new VBox(20); manualEntryCard.setMinWidth(380);
        manualEntryCard.setStyle("-fx-background-color: white; -fx-padding: 25; -fx-background-radius: 15;");
        manualEntryCard.setEffect(cardShadow);

        GridPane formGrid = new GridPane(); formGrid.setHgap(15); formGrid.setVgap(15);
        TextField hName = new TextField(); hName.setPromptText("Hospital Name"); styleInputField(hName);
        ComboBox<String> cityC = new ComboBox<>(); cityC.getItems().addAll(new TreeSet<>(cityCoordinates.keySet())); cityC.setPromptText("City");
        ComboBox<String> bloodC = new ComboBox<>(); bloodC.getItems().addAll("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"); bloodC.setPromptText("Blood");
        TextField sF = new TextField(); sF.setPromptText("Supply"); TextField dF = new TextField(); dF.setPromptText("Demand");
        styleInputField(sF); styleInputField(dF);

        formGrid.add(new Label("Name:"), 0, 0); formGrid.add(hName, 1, 0);
        formGrid.add(new Label("City:"), 0, 1); formGrid.add(cityC, 1, 1);
        formGrid.add(new Label("Blood:"), 0, 2); formGrid.add(bloodC, 1, 2);
        formGrid.add(new Label("Supply:"), 0, 3); formGrid.add(sF, 1, 3);
        formGrid.add(new Label("Demand:"), 0, 4); formGrid.add(dF, 1, 4);

        Button addB = new Button("Add Data"); stylePrimaryButton(addB, "#2980b9", "#1c5982"); addB.setMaxWidth(Double.MAX_VALUE);
        Label status = new Label();
        addB.setOnAction(e -> {
            try {
                double[] c = getCoordinatesFromCity(cityC.getValue());
                donorTree.insert(new Donor(hName.getText(), bloodC.getValue(), c[0], c[1], Integer.parseInt(sF.getText()), Integer.parseInt(dF.getText())));
                status.setText("✅ Added Successfully!"); status.setTextFill(Color.GREEN);
            } catch (Exception ex) { status.setText("❌ Error Check Inputs."); status.setTextFill(Color.RED); }
        });
        manualEntryCard.getChildren().addAll(new Label("Manual Hospital Entry"), new Separator(), formGrid, addB, status);

        VBox bulkCard = new VBox(25); bulkCard.setMinWidth(300);
        bulkCard.setStyle("-fx-background-color: white; -fx-padding: 25; -fx-background-radius: 15;");
        bulkCard.setEffect(cardShadow);

        Button loadB = new Button("Upload CSV"); stylePrimaryButton(loadB, "#f39c12", "#d35400");
        Label bulkStatus = new Label("No file loaded.");
        loadB.setOnAction(e -> {
            java.io.File f = new FileChooser().showOpenDialog(stage);
            if(f != null) {
                bulkStatus.setText("⏳ Processing 150+ records...");
                Thread importThread = new Thread(() -> {
                    donorTree = new RedBlackTree(); 
                    int count = loadDatabaseFromCSV(f.getAbsolutePath());
                    Platform.runLater(() -> {
                        bulkStatus.setText("✅ Loaded " + count + " records in seconds.");
                        bulkStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    });
                });
                importThread.setDaemon(true); importThread.start();
            }
        });
        bulkCard.getChildren().addAll(new Label("Bulk CSV Upload"), new Separator(), loadB, bulkStatus);

        contentArea.getChildren().addAll(manualEntryCard, bulkCard);
        mainLayout.getChildren().addAll(new Label("Database Administration Portal"), contentArea);
        return mainLayout;
    }

    // ==========================================
    // 3. VOLUNTEER REGISTRATION (IMPROVED UI)
    // ==========================================
    private void openVolunteerRegistrationPopup(Stage parentStage) {
        Stage popupStage = new Stage();
        popupStage.initOwner(parentStage);
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.setTitle("Volunteer Donor Registration");

        VBox mainLayout = new VBox(0);
        mainLayout.setStyle("-fx-background-color: white;");

        VBox eligibilityPane = new VBox(8);
        eligibilityPane.setPadding(new Insets(15));
        eligibilityPane.setStyle("-fx-background-color: #fdf2f2; -fx-border-color: #e74c3c; -fx-border-width: 0 0 2 0;");
        Label eTitle = new Label("Basic Donor Eligibility Criteria");
        eTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15)); eTitle.setTextFill(Color.web("#c0392b"));
        Label eList = new Label("• Aged 18–60 years.\n• Minimum 4 months between donations.\n• Weight above 50 kg.\n• Not pregnant; free from illness.");
        eList.setTextFill(Color.web("#7f8c8d")); eligibilityPane.getChildren().addAll(eTitle, eList);

        GridPane grid = new GridPane(); grid.setPadding(new Insets(20)); grid.setHgap(10); grid.setVgap(12);
        TextField n = new TextField(); n.setPromptText("Full Name"); styleInputField(n);
        TextField p = new TextField(); p.setPromptText("Phone Number"); styleInputField(p);
        ComboBox<String> city = new ComboBox<>(); city.getItems().addAll(new TreeSet<>(cityCoordinates.keySet())); city.setPromptText("City");
        ComboBox<String> b = new ComboBox<>(); b.getItems().addAll("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"); b.setPromptText("Blood");
        TextField a = new TextField(); a.setPromptText("Age"); styleInputField(a);
        TextField w = new TextField(); w.setPromptText("Weight"); styleInputField(w);
        CheckBox preg = new CheckBox("Are you currently pregnant?"); preg.setStyle("-fx-text-fill: #34495e;");

        grid.add(new Label("Name:"), 0, 0); grid.add(n, 1, 0);
        grid.add(new Label("Phone:"), 0, 1); grid.add(p, 1, 1);
        grid.add(new Label("City:"), 0, 2); grid.add(city, 1, 2);
        grid.add(new Label("Blood:"), 0, 3); grid.add(b, 1, 3);
        grid.add(new Label("Age:"), 0, 4); grid.add(a, 1, 4);
        grid.add(new Label("Weight:"), 0, 5); grid.add(w, 1, 5);
        grid.add(preg, 1, 6);

        VBox footer = new VBox(10); footer.setPadding(new Insets(10, 20, 20, 20));
        Button sub = new Button("Register HERO"); stylePrimaryButton(sub, "#27ae60", "#219150"); sub.setMaxWidth(Double.MAX_VALUE);
        Label status = new Label(); footer.getChildren().addAll(sub, status);

        sub.setOnAction(ev -> {
            try {
                int ageVal = Integer.parseInt(a.getText()); double weightVal = Double.parseDouble(w.getText());
                if (ageVal < 18 || weightVal < 50 || preg.isSelected()) { status.setText("❌ Criteria not met."); return; }
                double[] coords = getCoordinatesFromCity(city.getValue());
                Volunteer v = new Volunteer(n.getText(), b.getValue(), p.getText(), coords[0], coords[1], ageVal, weightVal, 0.0, preg.isSelected());
                volunteerDatabase.putIfAbsent(v.bloodType, new ArrayList<>());
                volunteerDatabase.get(v.bloodType).add(v); saveVolunteerToFile(v);
                status.setText("✅ Hero Registered!");
                new Timeline(new KeyFrame(Duration.seconds(1.5), e -> popupStage.close())).play();
            } catch (Exception ex) { status.setText("❌ Check inputs."); }
        });

        mainLayout.getChildren().addAll(eligibilityPane, grid, footer);
        popupStage.setScene(new Scene(mainLayout, 420, 620)); popupStage.show();
    }

    // ==========================================
    // 4. EMERGENCY DISPATCH QUEUE
    // ==========================================
    private VBox createEmergencyQueueUI() {
        VBox v = new VBox(15); v.setPadding(new Insets(30)); v.setAlignment(Pos.TOP_CENTER);
        v.getChildren().addAll(new Label("🚑 Emergency Dispatch Priority Queue"), queueListView);
        Button p = new Button("PROCESS NEXT EMERGENCY"); stylePrimaryButton(p, "#c0392b", "#a93226");
        p.setOnAction(e -> { if(!requestHeap.isEmpty()){ requestHeap.poll(); updateQueueDisplay(); } });
        v.getChildren().add(p); return v;
    }

    private void updateQueueDisplay() {
        queueListView.getItems().clear();
        for(EmergencyRequest r : requestHeap) queueListView.getItems().add(r.toString());
    }

    // ==========================================
    // 5. UTILITY & DATA HELPER METHODS
    // ==========================================
    private void initializeCityCoordinates() {
        cityCoordinates = new HashMap<>();
        cityCoordinates.put("Colombo", new double[]{6.9271, 79.8612});
        cityCoordinates.put("Kandy", new double[]{7.2906, 80.6337});
        cityCoordinates.put("Galle", new double[]{6.0535, 80.2210});
        cityCoordinates.put("Jaffna", new double[]{9.6615, 80.0255});
        cityCoordinates.put("Negombo", new double[]{7.2089, 79.8485});
        cityCoordinates.put("Gampaha", new double[]{7.0840, 80.0098});
        cityCoordinates.put("Kalutara", new double[]{6.5854, 79.9607});
        cityCoordinates.put("Kurunegala", new double[]{7.4818, 80.3609});
    }

    private double[] getCoordinatesFromCity(String n) {
        try {
            for (String k : cityCoordinates.keySet()) if (k.equalsIgnoreCase(n.trim())) return cityCoordinates.get(k);
            String u = "https://nominatim.openstreetmap.org/search?q=" + java.net.URLEncoder.encode(n.trim(), "UTF-8") + "%2CSri+Lanka&format=json&limit=1";
            HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
            c.setRequestProperty("User-Agent", "NIBMProject/1.0");
            Scanner s = new Scanner(c.getInputStream());
            if (!s.hasNext()) return null;
            String r = s.useDelimiter("\\A").next();
            if (r.contains("\"lat\":\"")) {
                double lat = Double.parseDouble(r.split("\"lat\":\"")[1].split("\"")[0]);
                double lon = Double.parseDouble(r.split("\"lon\":\"")[1].split("\"")[0]);
                double[] found = new double[]{lat, lon};
                cityCoordinates.put(n.trim(), found);
                return found;
            }
        } catch (Exception e) {} return null;
    }

    private int loadDatabaseFromCSV(String p) {
        int count = 0;
        try (Scanner sc = new Scanner(new java.io.File(p))) {
            if(sc.hasNextLine()) sc.nextLine();
            while(sc.hasNextLine()){
                String line = sc.nextLine(); if(line.trim().isEmpty()) continue;
                String[] d = line.split(",");
                if(d.length >= 5){
                    String cityName = d[1].trim();
                    if (!cityCoordinates.containsKey(cityName)) Thread.sleep(1000); 
                    double[] coords = getCoordinatesFromCity(cityName);
                    if(coords != null) {
                        donorTree.insert(new Donor(d[0].trim(), d[2].trim(), coords[0], coords[1], Integer.parseInt(d[3].trim()), Integer.parseInt(d[4].trim())));
                        count++;
                    }
                }
            }
        } catch (Exception e) {} return count;
    }

    private void styleInputField(TextField f) { f.setStyle("-fx-font-size: 14px; -fx-padding: 10; -fx-background-radius: 8; -fx-border-color: #bdc3c7;"); }

    private void stylePrimaryButton(Button b, String c, String h) {
        String s = "-fx-background-color: "+c+"; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12 25; -fx-background-radius: 8; -fx-cursor: hand;";
        b.setStyle(s); b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: "+h+";"+s.substring(29))); b.setOnMouseExited(e -> b.setStyle(s));
    }

    private void showVolunteerDetailsPopup(Volunteer v) {
        Stage s = new Stage(); s.setTitle("Donor Profile");
        GridPane g = new GridPane(); g.setPadding(new Insets(20)); g.setHgap(10); g.setVgap(10);
        g.add(new Label("Name: " + v.name), 0, 0); g.add(new Label("Age: " + v.age), 0, 1);
        g.add(new Label("Weight: " + v.weight + " kg"), 0, 2); g.add(new Label("Blood: " + v.bloodType), 0, 3);
        s.setScene(new Scene(g, 280, 200)); s.show();
    }

    private void saveVolunteerToFile(Volunteer v) {
        try (BufferedWriter w = new BufferedWriter(new FileWriter("volunteers.txt", true))) {
            w.write(v.toCSV()); w.newLine();
        } catch (IOException e) {}
    }

    private Label createAlertMessage(String t) { Label l = new Label(t); l.setStyle("-fx-text-fill: #c0392b; -fx-background-color: #fadbd8; -fx-padding: 10; -fx-background-radius: 8;"); return l; }

    private List<String> getCompatibleBloodTypes(String t) {
        switch (t) {
            case "O-": return Arrays.asList("O-"); case "O+": return Arrays.asList("O+", "O-");
            case "A-": return Arrays.asList("A-", "O-"); case "A+": return Arrays.asList("A+", "A-", "O+", "O-");
            case "B-": return Arrays.asList("B-", "O-"); case "B+": return Arrays.asList("B+", "B-", "O+", "O-");
            case "AB-": return Arrays.asList("AB-", "A-", "B-", "O-"); case "AB+": return Arrays.asList("AB+", "AB-", "A+", "A-", "B+", "B-", "O+", "O-");
            default: return new ArrayList<>();
        }
    }

    private double calculateDistance(double la1, double lo1, double la2, double lo2) {
        double dLat = Math.toRadians(la2 - la1), dLon = Math.toRadians(lo2 - lo1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(la1)) * Math.cos(Math.toRadians(la2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371 * (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }

    public static void main(String[] args) { launch(args); }
    private static class HospitalResult { Donor donor; double distance; public HospitalResult(Donor d, double dist) { this.donor = d; this.distance = dist; } }
    private static class VolunteerResult { Volunteer volunteer; double distance; public VolunteerResult(Volunteer v, double dist) { this.volunteer = v; this.distance = dist; } }
}