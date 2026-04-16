import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class MapDashboard extends Application {
    
    private RedBlackTree donorTree; 

    @Override
    public void start(Stage stage) {
        
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

  
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        
        TextField bloodField = new TextField();
        bloodField.setPromptText("Blood Type");
        bloodField.setStyle("-fx-pref-width: 100;");

        TextField cityField = new TextField();
        cityField.setPromptText("Your City (e.g. Jaffna)");
        cityField.setStyle("-fx-pref-width: 200;");
        
        Button searchButton = new Button("FIND & TRACK ROUTE");
        searchButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        Label statusLabel = new Label("Ready to track...");
        statusLabel.setStyle("-fx-text-fill: #ecf0f1;");

  
        File file = new File("src/resources/map.html"); 
        webEngine.load(file.toURI().toString());


        searchButton.setOnAction(e -> {
            String type = bloodField.getText().toUpperCase().trim();
            String city = cityField.getText().trim();
            
            Donor found = donorTree.search(type);

            if (found != null && !city.isEmpty()) {
                statusLabel.setText("Searching for " + city + "...");
                
                
                double[] cityCoords = getCoordinatesFromCity(city);
                
                if (cityCoords != null) {
                    String script = String.format("drawRoute(%f, %f, %f, %f)", 
                                    cityCoords[0], cityCoords[1], found.lat, found.lon);
                    webEngine.executeScript(script);
                    statusLabel.setText("Route Found: " + city + " to " + found.name);
                } else {
                    statusLabel.setText("Error: City not found.");
                }
            } else {
                statusLabel.setText("No " + type + " donor found in system.");
            }
        });

        HBox inputBar = new HBox(15, new Label("Blood:"), bloodField, new Label("Location:"), cityField, searchButton, statusLabel);
        inputBar.setStyle("-fx-padding: 20; -fx-background-color: #2c3e50; -fx-alignment: center-left;");
        inputBar.getChildren().stream().filter(n -> n instanceof Label).forEach(n -> n.setStyle("-fx-text-fill: white; -fx-font-weight: bold;"));

        VBox layout = new VBox(inputBar, webView);
        VBox.setVgrow(webView, Priority.ALWAYS);
        
        Scene scene = new Scene(layout, 1200, 800); 
        stage.setTitle("Emergency Blood Bank Locator Tracker - Sri Lanka");
        stage.setScene(scene);
        stage.show();
    }

    private double[] getCoordinatesFromCity(String cityName) {
        try {
            String urlStr = "https://nominatim.openstreetmap.org/search?q=" + cityName.replace(" ", "+") + ",Sri+Lanka&format=json&limit=1";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "JavaFX-App");

            Scanner scanner = new Scanner(conn.getInputStream());
            String response = scanner.useDelimiter("\\A").next();
            scanner.close();

            double lat = Double.parseDouble(response.split("\"lat\":\"")[1].split("\"")[0]);
            double lon = Double.parseDouble(response.split("\"lon\":\"")[1].split("\"")[0]);
            
            return new double[]{lat, lon};
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}