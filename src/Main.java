import java.util.*;


public class Main {
    public static void main(String[] args) {
      
        Graph g = new Graph();

        g.addEdge("A", "B", 4);
        g.addEdge("A", "C", 6);
        g.addEdge("A", "D", 7);
        g.addEdge("B", "C", 2);
        g.addEdge("B", "E", 5);
        g.addEdge("C", "D", 3);
        g.addEdge("C", "E", 4);
        g.addEdge("D", "F", 2);
        g.addEdge("E", "G", 3);

        System.out.println("--- Hospital & Blood Bank Network ---");
        g.display();

       
        String startPoint = "A";
        System.out.println("\n--- Calculating Shortest Paths from " + startPoint + " ---");
        
    
        Map<String, Integer> shortestPaths = Dijkstra.findShortestPaths(g, startPoint);

  
        System.out.println("Destination | Travel Time (Minutes)");
        System.out.println("-----------------------------------");
        for (String destination : shortestPaths.keySet()) {
            int time = shortestPaths.get(destination);
            String timeDisplay = (time == Integer.MAX_VALUE) ? "Unreachable" : time + " mins";
            System.out.println("Location " + destination + " : " + timeDisplay);
        }
    }
}