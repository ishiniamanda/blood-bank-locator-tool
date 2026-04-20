package app;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import model.*;

import java.util.*;

public class Controller {

    // ================= MAP UI =================
    @FXML private TextField startField;
    @FXML private TextField bloodField;
    @FXML private TextArea outputArea;
    @FXML private WebView mapView;

    private WebEngine engine;

    // ================= DATA STRUCTURES =================
    private Graph g = new Graph();
    private BloodFilter bloodFilter = new BloodFilter();
    private RedBlackTree donorTree = new RedBlackTree();
    private PriorityQueueManager pq = new PriorityQueueManager();

    private Map<String, double[]> locations = new HashMap<>();

    // ================= OPEN DONOR VIEW =================
    @FXML
    public void openDonorView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/donor-view.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Donor Panel");
            stage.setScene(new Scene(root, 700, 500));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= INIT =================
    @FXML
    public void initialize() {

        engine = mapView.getEngine();

        // GRAPH
        g.addEdge("A","B",4);
        g.addEdge("B","E",5);
        g.addEdge("A","C",6);
        g.addEdge("C","E",4);

        // LOCATIONS
        locations.put("A", new double[]{7.29,80.63});
        locations.put("B", new double[]{6.92,79.86});
        locations.put("C", new double[]{6.05,80.22});
        locations.put("E", new double[]{7.87,80.77});

        // BLOOD DATA
        bloodFilter.addHospital("A","A+");
        bloodFilter.addHospital("B","B+");
        bloodFilter.addHospital("C","A+");
        bloodFilter.addHospital("E","O+");

        // RED BLACK TREE
        donorTree.insert("A+","Donor A");
        donorTree.insert("B+","Donor B");
        donorTree.insert("O+","Donor C");

        // PRIORITY QUEUE
        pq.addDonor(new Donor("Kasun", "A+", 1));
        pq.addDonor(new Donor("Nimal", "O+", 3));
        pq.addDonor(new Donor("Saman", "B+", 2));

        loadMap(null);
    }

    // ================= SEARCH =================
    @FXML
    public void handleSearch() {

        String start = startField.getText().toUpperCase();
        String blood = bloodField.getText().toUpperCase();

        Dijkstra.Result res = Dijkstra.findShortestPaths(g, start);

        List<String> hospitals = bloodFilter.getHospitalsWithBlood(blood);

        String nearest = null;
        int min = Integer.MAX_VALUE;

        for (String h : hospitals) {
            int d = res.distances.getOrDefault(h, Integer.MAX_VALUE);
            if (d < min) {
                min = d;
                nearest = h;
            }
        }

        if (nearest != null) {

            List<String> path = Dijkstra.getPath(res.previous, nearest);

            Donor nextDonor = pq.getNextDonor();
            String donor = (nextDonor != null)
                    ? nextDonor.getName()
                    : "No donor available";

            outputArea.setText(
                "Hospital: " + nearest +
                "\nDistance: " + min +
                "\nPath: " + String.join(" → ", path) +
                "\nNext Donor: " + donor
            );

            loadMap(path);
        }
    }

    // ================= MAP =================
    private void loadMap(List<String> path) {

        StringBuilder markers = new StringBuilder();

        for (String k : locations.keySet()) {
            double[] c = locations.get(k);

            markers.append(
                "L.marker(["+c[0]+","+c[1]+"]).addTo(map).bindPopup('"+k+"');"
            );
        }

        String poly = "";

        if (path != null && path.size() > 1) {

            StringBuilder p = new StringBuilder("[");

            for (String n : path) {
                double[] c = locations.get(n);
                p.append("[").append(c[0]).append(",").append(c[1]).append("],");
            }

            p.append("]");

            poly = "L.polyline(" + p + ", {color:'red', weight:4}).addTo(map);";
        }

        String html =
            "<html><head>" +
            "<link rel='stylesheet' href='https://unpkg.com/leaflet/dist/leaflet.css'/>" +
            "<script src='https://unpkg.com/leaflet/dist/leaflet.js'></script>" +
            "</head><body style='margin:0'>" +

            "<div id='map' style='width:100%; height:100vh;'></div>" +

            "<script>" +

            "var map = L.map('map').setView([7,80],7);" +

            "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {" +
            "attribution:'© OpenStreetMap'}).addTo(map);" +

            markers.toString() +
            poly +

            "setTimeout(function(){ map.invalidateSize(); }, 300);" +

            "</script></body></html>";

        engine.loadContent(html);
    }
}