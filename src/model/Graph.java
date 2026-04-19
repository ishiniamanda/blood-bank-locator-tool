package model;

import java.util.*;

public class Graph {

    public static class Edge {
        public String node;
        public int weight;

        public Edge(String node, int weight) {
            this.node = node;
            this.weight = weight;
        }
    }

    private Map<String, List<Edge>> adj = new HashMap<>();

    public void addEdge(String from, String to, int weight) {
        adj.putIfAbsent(from, new ArrayList<>());
        adj.putIfAbsent(to, new ArrayList<>());

        adj.get(from).add(new Edge(to, weight));
        adj.get(to).add(new Edge(from, weight)); // undirected
    }

    public Map<String, List<Edge>> getAdj() {
        return adj;
    }
}