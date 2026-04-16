import java.util.*;

public class Main {
    public static void main(String[] args) {

        Graph g = new Graph();

        // GRAPH DATA
        g.addEdge("A", "B", 4);
        g.addEdge("A", "C", 6);
        g.addEdge("A", "D", 7);
        g.addEdge("B", "C", 2);
        g.addEdge("B", "E", 5);
        g.addEdge("C", "D", 3);
        g.addEdge("C", "E", 4);
        g.addEdge("D", "F", 2);
        g.addEdge("E", "G", 3);

        Scanner sc = new Scanner(System.in);

        System.out.print("Enter start node: ");
        String start = sc.nextLine();

        // CALL PARTNER DIJKSTRA
        Map<String, Integer> result = Dijkstra.findShortestPaths(g, start);

        // PRINT RESULT
        System.out.println("\nShortest Distances from " + start + ":");

        for (Map.Entry<String, Integer> entry : result.entrySet()) {
            System.out.println(start + " -> " + entry.getKey() + " = " + entry.getValue());
        }
    }
}