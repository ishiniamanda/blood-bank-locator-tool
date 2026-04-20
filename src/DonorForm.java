import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class DonorForm {

    private RedBlackTree donorTree;
    private MapDashboard dashboard; // Added to access save method

    // Constructor updated to accept MapDashboard
    public DonorForm(RedBlackTree donorTree, MapDashboard dashboard) {
        this.donorTree = donorTree;
        this.dashboard = dashboard;
    }

    public void show() {

        Stage stage = new Stage();

        VBox layout = new VBox(12);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);

        Label title = new Label("Donor Registration Form");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        TextField nameField = new TextField();
        nameField.setPromptText("Full Name");

        TextField bloodField = new TextField();
        bloodField.setPromptText("Blood Type (A+, O-, etc)");

        TextField cityField = new TextField();
        cityField.setPromptText("City");

        TextField ageField = new TextField();
        ageField.setPromptText("Age");

        TextField weightField = new TextField();
        weightField.setPromptText("Weight (kg)");

        TextField hbField = new TextField();
        hbField.setPromptText("Hemoglobin level");

        CheckBox pregnantBox = new CheckBox("Currently Pregnant");

        Button submitBtn = new Button("Register Donor");
        submitBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");

        Label status = new Label();

        submitBtn.setOnAction(e -> {

            try {
                String name = nameField.getText().trim();
                String blood = bloodField.getText().toUpperCase().trim();
                String city = cityField.getText().trim();

                int age = Integer.parseInt(ageField.getText().trim());
                double weight = Double.parseDouble(weightField.getText().trim());
                double hb = Double.parseDouble(hbField.getText().trim());
                boolean pregnant = pregnantBox.isSelected();

                // ✅ Basic Validation
                if (name.isEmpty() || blood.isEmpty() || city.isEmpty()) {
                    status.setText("Please fill all fields.");
                    return;
                }

                if (age < 18 || age > 60) {
                    status.setText("Age must be between 18 and 60.");
                    return;
                }

                if (weight < 50) {
                    status.setText("Weight must be at least 50kg.");
                    return;
                }

                if (hb < 12) {
                    status.setText("Hemoglobin must be at least 12.");
                    return;
                }

                if (pregnant) {
                    status.setText("Pregnant donors are not eligible.");
                    return;
                }

                // ✅ Get Coordinates
                double[] coords = getCoordinatesFromCity(city);

                if (coords == null) {
                    status.setText("City not found. Try another.");
                    return;
                }

                // ✅ Create donor
                Donor donor = new Donor(
                        name, blood,
                        coords[0], coords[1],
                        age, weight, hb, pregnant
                );

                // 1. Insert into Red-Black Tree (For current session search)
                donorTree.insert(donor);

                // 2. Save to donor.txt (For permanent storage)
                dashboard.saveDonorToFile(donor); 

                status.setText("✅ Donor registered and saved to file!");

                // Clear form
                nameField.clear();
                bloodField.clear();
                cityField.clear();
                ageField.clear();
                weightField.clear();
                hbField.clear();
                pregnantBox.setSelected(false);

            } catch (NumberFormatException ex) {
                status.setText("Invalid number format.");
            } catch (Exception ex) {
                status.setText("Error occurred.");
            }
        });

        layout.getChildren().addAll(
                title,
                nameField,
                bloodField,
                cityField,
                ageField,
                weightField,
                hbField,
                pregnantBox,
                submitBtn,
                status
        );

        Scene scene = new Scene(layout, 400, 500);
        stage.setTitle("Donor Registration");
        stage.setScene(scene);
        stage.show();
    }

    // ================= LOCATION METHOD =================
    private double[] getCoordinatesFromCity(String cityName) {
        try {
            String safeCityName = java.net.URLEncoder.encode(cityName.trim(), "UTF-8");
            String urlStr = "https://nominatim.openstreetmap.org/search?q=" + safeCityName + "%2CSri+Lanka&format=json&limit=1";

            java.net.URL url = new java.net.URL(urlStr);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "NIBM_Student_Project/1.0");

            java.util.Scanner scanner = new java.util.Scanner(conn.getInputStream());
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
}