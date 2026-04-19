package app;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.web.WebView;
import model.*;


import java.util.*;

public class Controller {

    @FXML private TextField startField;
    @FXML private TextField bloodField;
    @FXML private TextArea outputArea;
    @FXML private WebView mapView;
    

    private Graph g = new Graph();
    private Map<String, double[]> locations = new HashMap<>();

    @FXML
    public void initialize() {

        // GRAPH
        g.addEdge("A","B",4);
        g.addEdge("B","E",5);
        g.addEdge("A","C",6);
        g.addEdge("C","E",4);

        // REALISTIC COORDINATES (Sri Lanka)
        locations.put("A", new double[]{7.2906, 80.6337}); // Kandy
        locations.put("B", new double[]{6.9271, 79.8612}); // Colombo
        locations.put("C", new double[]{6.0535, 80.2210}); // Galle
        locations.put("E", new double[]{7.8731, 80.7718}); // Dambulla

        loadMap(null);
    }

    @FXML
    public void handleSearch() {

        String start = startField.getText().toUpperCase();
        String blood = bloodField.getText().toUpperCase();

        Dijkstra.Result res = Dijkstra.findShortestPaths(g, start);

        BloodFilter bf = new BloodFilter();
        List<String> hospitals = bf.getHospitalsWithBlood(blood);

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

            outputArea.setText(
                    "Nearest Hospital: " + nearest +
                    "\nDistance: " + min +
                    "\nPath: " + String.join(" → ", path)
            );

            loadMap(path);

        } else {
            outputArea.setText("No matching blood found.");
        }
    }

    // ================= REAL OPENSTREETMAP =================
    private void loadMap(List<String> path) {

        StringBuilder markers = new StringBuilder();

        for (String key : locations.keySet()) {
            double[] c = locations.get(key);

            markers.append(
                "L.marker([" + c[0] + "," + c[1] + "])"
              + ".addTo(map).bindPopup('" + key + "');"
            );
        }

        String polyline = "";

        if (path != null && path.size() > 1) {
            StringBuilder line = new StringBuilder("[");
            for (String p : path) {
                double[] c = locations.get(p);
                line.append("[").append(c[0]).append(",").append(c[1]).append("],");
            }
            line.append("]");
            polyline =
                "L.polyline(" + line + ", {color:'red', weight:5}).addTo(map);";
        }

        String html =
            "<html>" +
            "<head>" +
            "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>" +
            "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
            "</head>" +
            "<body style='margin:0'>" +
            "<div id='map' style='width:100%; height:100vh;'></div>" +
            "<script>" +

            "var map = L.map('map').setView([7.0,80.7], 7);" +

            // REAL OPENSTREETMAP TILES
            "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {" +
            "attribution: '© OpenStreetMap contributors'" +
            "}).addTo(map);" +

            markers.toString() +
            polyline +

            "</script>" +
            "</body></html>";

        mapView.getEngine().loadContent(html);
    }
}