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

    // Core Data Structures
    private RedBlackTree donorTree; 
    private HashMap<String, ArrayList<Volunteer>> volunteerDatabase; 
    private HashMap<String, double[]> cityCoordinates; 
    private PriorityQueue<EmergencyRequest> requestHeap;
    private ListView<String> queueListView; 

    // UI Styles
    private final String CARD_STYLE = "-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 30;";
    private final String BG_COLOR = "-fx-background-color: #f0f4f8;";
    private final DropShadow softShadow = new DropShadow(20, Color.rgb(0, 0, 0, 0.05));

    // UI Containers
    private StackPane centerArea;
    private VBox patientView, queueView, adminView;

    @Override
    public void start(Stage stage) {
        donorTree = new RedBlackTree();
        volunteerDatabase = new HashMap<>();
        requestHeap = new PriorityQueue<>();
        queueListView = new ListView<>();
        initializeCityCoordinates();

        // Initialize Views
        patientView = createPatientDashboard(stage);
        queueView = createEmergencyQueueUI();
        adminView = createAdminPortal(stage);

        // Main Layout Setup
        BorderPane root = new BorderPane();
        root.setStyle(BG_COLOR + " -fx-font-family: 'Segoe UI', Arial, sans-serif;");

        // 1. Create Sidebar
        VBox sidebar = createSidebar(stage);
        root.setLeft(sidebar);

        // 2. Create Center Area
        centerArea = new StackPane();
        centerArea.setPadding(new Insets(20));
        centerArea.getChildren().add(patientView); // Default view
        root.setCenter(centerArea);

        Scene scene = new Scene(root, 950, 700);
        stage.setTitle("Emergency Blood Bank System - Advanced UI");
        stage.setScene(scene);
        stage.show();
    }

    // ==========================================
    // MODERN SIDEBAR NAVIGATION
    // ==========================================
    private VBox createSidebar(Stage stage) {
        VBox sidebar = new VBox(15);
        sidebar.setPrefWidth(240);
        sidebar.setStyle("-fx-background-color: #1a252f; -fx-padding: 30 15 30 15;");
        sidebar.setAlignment(Pos.TOP_CENTER);

        // Logo / Title area
        Label logo = new Label("🩸 NIBM");
        logo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        logo.setTextFill(Color.web("#e74c3c"));
        Label subTitle = new Label("Blood Bank System");
        subTitle.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        subTitle.setTextFill(Color.web("#bdc3c7"));
        
        VBox brandBox = new VBox(5, logo, subTitle);
        brandBox.setAlignment(Pos.CENTER);
        brandBox.setPadding(new Insets(0, 0, 40, 0));

        // Navigation Buttons
        Button btnPatient = createNavButton("🔍 Search & Route");
        Button btnQueue = createNavButton("🚑 Dispatch Queue");
        Button btnAdmin = createNavButton("🔒 Admin Portal");

        // View Switching Logic
        btnPatient.setOnAction(e -> switchView(patientView));
        btnQueue.setOnAction(e -> switchView(queueView));
        btnAdmin.setOnAction(e -> {
            if (showAdminLoginDialog(stage)) {
                switchView(adminView);
            }
        });

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Label version = new Label("v2.0 Advanced");
        version.setTextFill(Color.web("#7f8c8d"));

        sidebar.getChildren().addAll(brandBox, btnPatient, btnQueue, btnAdmin, spacer, version);
        return sidebar;
    }

    private Button createNavButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(12, 15, 12, 15));
        String idleStyle = "-fx-background-color: transparent; -fx-text-fill: #ecf0f1; -fx-font-size: 15px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 8;";
        String hoverStyle = "-fx-background-color: #34495e; -fx-text-fill: #ffffff; -fx-font-size: 15px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 8;";
        btn.setStyle(idleStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(idleStyle));
        return btn;
    }

    private void switchView(VBox view) {
        centerArea.getChildren().clear();
        centerArea.getChildren().add(view);
    }

    // ==========================================
    // SECURE ADMIN LOGIN
    // ==========================================
    private boolean showAdminLoginDialog(Stage owner) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Secure Portal");
        dialog.setHeaderText("Admin Authentication Required");
        dialog.initOwner(owner);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter Admin Password");
        styleInputField(passwordField);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20, 10, 10, 10));
        content.getChildren().addAll(new Label("Database Password:"), passwordField);

        dialog.getDialogPane().setContent(content);

        ButtonType loginButton = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButton, ButtonType.CANCEL);

        Platform.runLater(passwordField::requestFocus);

        dialog.setResultConverter(button -> {
            if (button == loginButton) return passwordField.getText();
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            if ("admin123".equals(result.get())) return true;
            createAlertMessage("Access Denied: Incorrect Password!");
        }
        return false;
    }

    // ==========================================
    // 1. PATIENT DASHBOARD (MODERN CARD Layout)
    // ==========================================
    private VBox createPatientDashboard(Stage stage) {
        VBox layout = new VBox(25);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setStyle("-fx-background-color: transparent;");

        VBox headerBox = new VBox(5);
        Label titleLabel = new Label("Emergency Locator");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        titleLabel.setTextFill(Color.web("#2c3e50"));
        Label subLabel = new Label("Find the closest compatible blood match instantly.");
        subLabel.setTextFill(Color.web("#7f8c8d"));
        headerBox.getChildren().addAll(titleLabel, subLabel);
        
        // Search Form Card
        VBox searchCard = new VBox(20);
        searchCard.setStyle(CARD_STYLE);
        searchCard.setEffect(softShadow);
        searchCard.setAlignment(Pos.CENTER);

        HBox inputsBox = new HBox(15);
        inputsBox.setAlignment(Pos.CENTER);
        TextField bloodField = new TextField(); bloodField.setPromptText("Blood Type (e.g., A+)");
        TextField cityField = new TextField(); cityField.setPromptText("Your City (e.g., Kandy)");
        bloodField.setPrefWidth(200); cityField.setPrefWidth(250);
        styleInputField(bloodField); styleInputField(cityField);
        inputsBox.getChildren().addAll(bloodField, cityField);
        
        HBox btnRow = new HBox(15);
        btnRow.setAlignment(Pos.CENTER);
        Button searchButton = new Button("SEARCH EMERGENCY");
        stylePrimaryButton(searchButton, "#e74c3c", "#c0392b");
        Button registerBtn = new Button("BECOME A DONOR");
        stylePrimaryButton(registerBtn, "#27ae60", "#2ecc71");
        registerBtn.setOnAction(e -> openVolunteerRegistrationPopup(stage));
        btnRow.getChildren().addAll(searchButton, registerBtn);

        searchCard.getChildren().addAll(inputsBox, btnRow);

        // Results Area
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
                resultsBox.getChildren().add(createAlertMessage("Location not found."));
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
                hospitalResults.sort(Comparator.comparingDouble(a -> a.distance));
                VBox cards = new VBox(12);
                for (HospitalResult res : hospitalResults) {
                    HBox card = new HBox(20);
                    card.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 20; -fx-background-radius: 8; -fx-border-color: #e0e0e0; -fx-border-radius: 8;");
                    card.setAlignment(Pos.CENTER_LEFT);
                    
                    VBox info = new VBox(5);
                    Label nameLabel = new Label(res.donor.name + " (" + res.donor.bloodType + ")");
                    nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
                    nameLabel.setTextFill(res.donor.bloodType.equals(patientType) ? Color.web("#27ae60") : Color.web("#2980b9"));
                    
                    HBox statsBox = new HBox(15);
                    Label supplyLbl = new Label("📦 Units: " + res.donor.supply);
                    supplyLbl.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
                    Label distLbl = new Label(String.format("📍 %.1f km away", res.distance));
                    distLbl.setStyle("-fx-text-fill: #7f8c8d;");
                    statsBox.getChildren().addAll(supplyLbl, distLbl);
                    
                    info.getChildren().addAll(nameLabel, statsBox);
                    
                    Region s = new Region(); HBox.setHgrow(s, Priority.ALWAYS);
                    
                    Button map = new Button("Start Route 🗺️"); 
                    stylePrimaryButton(map, "#3498db", "#2980b9");
                    map.setOnAction(ev -> {
                        String start = userCity.replace(" ", "+");
                        String dest = res.donor.lat + "," + res.donor.lon;
                        String url = "https://www.google.com/maps/dir/?api=1&origin=" + start + "&destination=" + dest + "&travelmode=driving";
                        getHostServices().showDocument(url);
                    });

                    card.getChildren().addAll(info, s, map); 
                    cards.getChildren().add(card);
                }
                ScrollPane scroll = new ScrollPane(cards);
                scroll.setPrefHeight(380); 
                scroll.setStyle("-fx-background-color: transparent;");
                scroll.setFitToWidth(true);
                resultsBox.getChildren().add(scroll);
            }
        });
        
        layout.getChildren().addAll(headerBox, searchCard, resultsBox);
        return layout;
    }

    // ==========================================
    // 2. ADMIN PORTAL (CLEAN DASHBOARD)
    // ==========================================
    private VBox createAdminPortal(Stage stage) {
        VBox layout = new VBox(25);
        layout.setAlignment(Pos.TOP_LEFT);
        
        Label dashTitle = new Label("System Administration");
        dashTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        dashTitle.setTextFill(Color.web("#2c3e50"));

        HBox contentArea = new HBox(30); 
        contentArea.setAlignment(Pos.TOP_LEFT);

        VBox manualEntryCard = new VBox(20); 
        manualEntryCard.setMinWidth(400);
        manualEntryCard.setStyle(CARD_STYLE);
        manualEntryCard.setEffect(softShadow);

        Label mTitle = new Label("Manual Entry");
        mTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        
        GridPane formGrid = new GridPane(); formGrid.setHgap(15); formGrid.setVgap(15);
        TextField hName = new TextField(); hName.setPromptText("Hospital Name"); styleInputField(hName);
        ComboBox<String> cityC = new ComboBox<>(); cityC.getItems().addAll(new TreeSet<>(cityCoordinates.keySet())); cityC.setPromptText("Select City"); cityC.setPrefWidth(200);
        ComboBox<String> bloodC = new ComboBox<>(); bloodC.getItems().addAll("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"); bloodC.setPromptText("Blood Group"); bloodC.setPrefWidth(200);
        TextField sF = new TextField(); sF.setPromptText("Units Available"); TextField dF = new TextField(); dF.setPromptText("Units Needed");
        styleInputField(sF); styleInputField(dF);

        formGrid.add(new Label("Hospital:"), 0, 0); formGrid.add(hName, 1, 0);
        formGrid.add(new Label("City:"), 0, 1); formGrid.add(cityC, 1, 1);
        formGrid.add(new Label("Blood:"), 0, 2); formGrid.add(bloodC, 1, 2);
        formGrid.add(new Label("Supply:"), 0, 3); formGrid.add(sF, 1, 3);
        formGrid.add(new Label("Demand:"), 0, 4); formGrid.add(dF, 1, 4);

        Button addB = new Button("Insert Record"); 
        stylePrimaryButton(addB, "#2980b9", "#1c5982"); 
        addB.setMaxWidth(Double.MAX_VALUE);
        
        Label status = new Label();
        addB.setOnAction(e -> {
            try {
                double[] c = getCoordinatesFromCity(cityC.getValue());
                donorTree.insert(new Donor(hName.getText(), bloodC.getValue(), c[0], c[1], Integer.parseInt(sF.getText()), Integer.parseInt(dF.getText())));
                status.setText("✅ Database Updated Successfully!"); status.setTextFill(Color.web("#27ae60"));
            } catch (Exception ex) { status.setText("❌ Check Inputs."); status.setTextFill(Color.web("#e74c3c")); }
        });
        manualEntryCard.getChildren().addAll(mTitle, new Separator(), formGrid, addB, status);

        VBox bulkCard = new VBox(20); 
        bulkCard.setMinWidth(300);
        bulkCard.setStyle(CARD_STYLE);
        bulkCard.setEffect(softShadow);

        Label bTitle = new Label("Bulk Import");
        bTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

        Button loadB = new Button("Upload CSV File"); 
        stylePrimaryButton(loadB, "#f39c12", "#d35400");
        Label bulkStatus = new Label("Waiting for file...");
        bulkStatus.setTextFill(Color.web("#7f8c8d"));

        loadB.setOnAction(e -> {
            java.io.File f = new FileChooser().showOpenDialog(stage);
            if(f != null) {
                bulkStatus.setText("⏳ Processing dataset...");
                Thread importThread = new Thread(() -> {
                    donorTree = new RedBlackTree(); 
                    int count = loadDatabaseFromCSV(f.getAbsolutePath());
                    Platform.runLater(() -> {
                        bulkStatus.setText("✅ Indexed " + count + " nodes into Red-Black Tree.");
                        bulkStatus.setTextFill(Color.web("#27ae60"));
                    });
                });
                importThread.setDaemon(true); importThread.start();
            }
        });
        bulkCard.getChildren().addAll(bTitle, new Separator(), loadB, bulkStatus);

        contentArea.getChildren().addAll(manualEntryCard, bulkCard);
        layout.getChildren().addAll(dashTitle, contentArea);
        return layout;
    }

    // ==========================================
    // VOLUNTEER REGISTRATION 
    // ==========================================
    private void openVolunteerRegistrationPopup(Stage parentStage) {
        Stage popupStage = new Stage();
        popupStage.initOwner(parentStage);
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.setTitle("Hero Registration");

        VBox mainLayout = new VBox(0);
        mainLayout.setStyle("-fx-background-color: white;");

        VBox eligibilityPane = new VBox(8);
        eligibilityPane.setPadding(new Insets(20));
        eligibilityPane.setStyle("-fx-background-color: #fff5f5; -fx-border-color: #ffcccc; -fx-border-width: 0 0 1 0;");
        Label eTitle = new Label("Eligibility Requirements");
        eTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16)); eTitle.setTextFill(Color.web("#e74c3c"));
        Label eList = new Label("• 18–60 years old\n• Minimum 4 months since last donation\n• Over 50 kg\n• Not pregnant");
        eList.setTextFill(Color.web("#555555")); eligibilityPane.getChildren().addAll(eTitle, eList);

        GridPane grid = new GridPane(); grid.setPadding(new Insets(25)); grid.setHgap(15); grid.setVgap(15);
        TextField n = new TextField(); n.setPromptText("Full Name"); styleInputField(n);
        TextField p = new TextField(); p.setPromptText("Phone Number"); styleInputField(p);
        ComboBox<String> city = new ComboBox<>(); city.getItems().addAll(new TreeSet<>(cityCoordinates.keySet())); city.setPromptText("City");
        ComboBox<String> b = new ComboBox<>(); b.getItems().addAll("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"); b.setPromptText("Blood");
        TextField a = new TextField(); a.setPromptText("Age"); styleInputField(a);
        TextField w = new TextField(); w.setPromptText("Weight"); styleInputField(w);
        CheckBox preg = new CheckBox("Currently pregnant?"); preg.setStyle("-fx-text-fill: #34495e; -fx-font-size: 13px;");

        grid.add(new Label("Name:"), 0, 0); grid.add(n, 1, 0);
        grid.add(new Label("Phone:"), 0, 1); grid.add(p, 1, 1);
        grid.add(new Label("City:"), 0, 2); grid.add(city, 1, 2);
        grid.add(new Label("Blood:"), 0, 3); grid.add(b, 1, 3);
        grid.add(new Label("Age:"), 0, 4); grid.add(a, 1, 4);
        grid.add(new Label("Weight:"), 0, 5); grid.add(w, 1, 5);
        grid.add(preg, 1, 6);

        VBox footer = new VBox(10); footer.setPadding(new Insets(10, 25, 25, 25));
        Button sub = new Button("Register Hero"); stylePrimaryButton(sub, "#27ae60", "#219150"); sub.setMaxWidth(Double.MAX_VALUE);
        Label status = new Label(); footer.getChildren().addAll(sub, status);

        sub.setOnAction(ev -> {
            try {
                int ageVal = Integer.parseInt(a.getText()); double weightVal = Double.parseDouble(w.getText());
                if (ageVal < 18 || weightVal < 50 || preg.isSelected()) { status.setText("❌ Criteria not met."); status.setTextFill(Color.RED); return; }
                double[] coords = getCoordinatesFromCity(city.getValue());
                Volunteer v = new Volunteer(n.getText(), b.getValue(), p.getText(), coords[0], coords[1], ageVal, weightVal, 0.0, preg.isSelected());
                volunteerDatabase.putIfAbsent(v.bloodType, new ArrayList<>());
                volunteerDatabase.get(v.bloodType).add(v); saveVolunteerToFile(v);
                status.setText("✅ Hero Registered Successfully!"); status.setTextFill(Color.GREEN);
                new Timeline(new KeyFrame(Duration.seconds(1.5), e -> popupStage.close())).play();
            } catch (Exception ex) { status.setText("❌ Missing or invalid inputs."); status.setTextFill(Color.RED); }
        });

        mainLayout.getChildren().addAll(eligibilityPane, grid, footer);
        popupStage.setScene(new Scene(mainLayout, 380, 580)); popupStage.show();
    }

    // ==========================================
    // EMERGENCY DISPATCH QUEUE
    // ==========================================
    private VBox createEmergencyQueueUI() {
        VBox v = new VBox(20); 
        v.setAlignment(Pos.TOP_LEFT);
        
        Label dashTitle = new Label("Active Dispatch Queue");
        dashTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        dashTitle.setTextFill(Color.web("#2c3e50"));
        
        VBox listCard = new VBox(15);
        listCard.setStyle(CARD_STYLE);
        listCard.setEffect(softShadow);
        
        queueListView.setPrefHeight(400);
        queueListView.setStyle("-fx-font-size: 16px; -fx-font-family: 'Segoe UI';");

        Button p = new Button("DISPATCH NEXT EMERGENCY"); 
        stylePrimaryButton(p, "#c0392b", "#a93226");
        p.setMaxWidth(Double.MAX_VALUE);
        
        p.setOnAction(e -> { if(!requestHeap.isEmpty()){ requestHeap.poll(); updateQueueDisplay(); } });
        
        listCard.getChildren().addAll(queueListView, p);
        v.getChildren().addAll(dashTitle, listCard); 
        return v;
    }

    private void updateQueueDisplay() {
        queueListView.getItems().clear();
        for(EmergencyRequest r : requestHeap) queueListView.getItems().add(r.toString());
    }

    // ==========================================
    // UTILITY & HELPER METHODS
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
        cityCoordinates.put("Kegalle", new double[]{7.2513, 80.3464});
        cityCoordinates.put("Peradeniya", new double[]{7.2683, 80.5933});
        cityCoordinates.put("Matale", new double[]{7.4675, 80.6234});
        cityCoordinates.put("Hambantota", new double[]{6.1246, 81.1185});
        cityCoordinates.put("Ampara", new double[]{7.2842, 81.6747});
        cityCoordinates.put("Puttalam", new double[]{8.0330, 79.8259});
        cityCoordinates.put("Ragama", new double[]{7.0263, 79.9142});
        cityCoordinates.put("Trincomalee", new double[]{8.5717, 81.2335});
        cityCoordinates.put("Matara", new double[]{5.9549, 80.5550});
        cityCoordinates.put("Kuliyapitiya", new double[]{7.4674, 80.0402});
        cityCoordinates.put("Kalubowila", new double[]{6.8653, 79.8733});
        cityCoordinates.put("Ratnapura", new double[]{6.6828, 80.3992});
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
            if (r.contains("\"lat\":\"")) return new double[]{Double.parseDouble(r.split("\"lat\":\"")[1].split("\"")[0]), Double.parseDouble(r.split("\"lon\":\"")[1].split("\"")[0])};
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
                        double hLat = coords[0] + ((Math.random() - 0.5) * 0.08);
                        double hLon = coords[1] + ((Math.random() - 0.5) * 0.08);
                        donorTree.insert(new Donor(d[0].trim(), d[2].trim(), hLat, hLon, Integer.parseInt(d[3].trim()), Integer.parseInt(d[4].trim())));
                        count++;
                    }
                }
            }
        } catch (Exception e) {} return count;
    }

    private void styleInputField(TextField f) { 
        f.setStyle("-fx-font-size: 15px; -fx-padding: 10 15 10 15; -fx-background-radius: 6; -fx-border-color: #bdc3c7; -fx-border-radius: 6; -fx-background-color: #fcfcfc;"); 
    }

    private void stylePrimaryButton(Button b, String color, String hover) {
        String base = "-fx-background-color: "+color+"; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12 25; -fx-background-radius: 6; -fx-cursor: hand;";
        b.setStyle(base); 
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: "+hover+";"+base.substring(29))); 
        b.setOnMouseExited(e -> b.setStyle(base));
    }

    private void saveVolunteerToFile(Volunteer v) {
        try (BufferedWriter w = new BufferedWriter(new FileWriter("volunteers.txt", true))) { w.write(v.toCSV()); w.newLine(); } catch (IOException e) {}
    }

    private Label createAlertMessage(String t) { 
        Label l = new Label("⚠️ " + t); 
        l.setStyle("-fx-text-fill: #c0392b; -fx-background-color: #fadbd8; -fx-padding: 12; -fx-background-radius: 8; -fx-font-weight: bold;"); 
        return l; 
    }

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
}