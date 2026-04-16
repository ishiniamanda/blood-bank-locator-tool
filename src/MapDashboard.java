import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;
import java.io.File;

public class MapDashboard extends Application {
    
    private RedBlackTree donorTree; 

    @Override
    public void start(Stage stage) {
        // 1. Setup the Red-Black Tree
        donorTree = new RedBlackTree();
        donorTree.insert(new Donor("National Blood Center", "A+", 6.9054, 79.8731));
        donorTree.insert(new Donor("Kandy General Hospital", "O+", 7.2906, 80.6337));
        donorTree.insert(new Donor("Galle Blood Bank", "B-", 6.0367, 80.2170));

        // 2. Setup the UI Components
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        
        // --- STYLING START ---
        TextField searchField = new TextField();
        searchField.setPromptText("Enter Blood Type (e.g., O+)");
        searchField.setStyle("-fx-pref-width: 250; -fx-background-radius: 5; -fx-padding: 8;");
        
        Button searchButton = new Button("FIND NEAREST DONOR");
        // Red button with white text and rounded corners
        searchButton.setStyle(
            "-fx-background-color: #e74c3c; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 5; " +
            "-fx-padding: 8 20 8 20; " +
            "-fx-cursor: hand;"
        );
        // --- STYLING END ---

        // 3. Load the Map
        File file = new File("src/resources/map.html"); 
        webEngine.load(file.toURI().toString());

        // 4. Button Logic
        searchButton.setOnAction(e -> {
            String type = searchField.getText().toUpperCase().trim();
            Donor found = donorTree.search(type);

            if (found != null) {
                String script = String.format("drawRoute(6.9271, 79.8612, %f, %f)", found.lat, found.lon);
                webEngine.executeScript(script);
            } else {
                System.out.println("No donor found for: " + type);
            }
        });

        // 5. Layout Styling
        HBox searchArea = new HBox(15, searchField, searchButton);
        // Dark blue-grey background for the header
        searchArea.setStyle(
            "-fx-padding: 20; " +
            "-fx-background-color: #2c3e50; " +
            "-fx-alignment: center-left; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 5);"
        );
        
        VBox layout = new VBox(searchArea, webView);
        VBox.setVgrow(webView, Priority.ALWAYS); // This makes the map fill the window
        
        Scene scene = new Scene(layout, 1000, 800); 
        stage.setTitle("Emergency Blood Bank Locator - Sri Lanka");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}