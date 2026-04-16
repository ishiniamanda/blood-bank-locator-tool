import java.util.*;

public class Graph {
    private Map<String, Map<String, Integer>> graph;

    public Graph() {
        graph = new HashMap<>();
    }

    public void addNode(String node) {
        graph.putIfAbsent(node, new HashMap<>());
    }

    public void addEdge(String from, String to, int distance) {
        graph.putIfAbsent(from, new HashMap<>());
        graph.putIfAbsent(to, new HashMap<>());

        graph.get(from).put(to, distance);
        graph.get(to).put(from, distance); // undirected
    }

    public Map<String, Map<String, Integer>> getGraph() {
        return graph;
    }

    public void display() {
        for (String node : graph.keySet()) {
            System.out.println(node + " -> " + graph.get(node));
        }
    }
}