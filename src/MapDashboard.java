import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;
import java.io.File;

public class MapDashboard extends Application {
    @Override
    public void start(Stage stage) {
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();

        // 1. CHANGE THIS PATH if your map.html is not inside a 'resources' folder
        // If your map.html is just inside 'src', change this to "src/map.html"
        File file = new File("src/resources/map.html"); 
        webEngine.load(file.toURI().toString());

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                
                // 2. CHANGE THESE COORDINATES
                // Let's draw a route from Colombo to Kandy to test the red line!
                double startLat = 6.9271;  // Colombo Latitude
                double startLon = 79.8612; // Colombo Longitude
                double endLat = 7.2906;    // Kandy Latitude
                double endLon = 80.6337;   // Kandy Longitude

                // This line sends the coordinates to the JavaScript function in your HTML
                String script = String.format("drawRoute(%f, %f, %f, %f)", startLat, startLon, endLat, endLon);
                webEngine.executeScript(script);
            }
        });

        // 3. CHANGE WINDOW SIZE if you want it bigger
        Scene scene = new Scene(webView, 1000, 800); 
        stage.setTitle("Emergency Blood Bank Locator - Route Visualization");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
