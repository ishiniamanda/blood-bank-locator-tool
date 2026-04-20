package model;

import java.util.*;

public class Graph {

    public Map<String, List<Edge>> adj = new HashMap<>();

    class Edge {
        String to;
        int weight;

        Edge(String t, int w) {
            to = t;
            weight = w;
        }
    }

    public void addEdge(String from, String to, int w) {

        adj.putIfAbsent(from, new ArrayList<>());
        adj.putIfAbsent(to, new ArrayList<>());

        adj.get(from).add(new Edge(to, w));
        adj.get(to).add(new Edge(from, w));
    }
}