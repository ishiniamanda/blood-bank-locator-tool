package model;

import java.util.*;

public class Dijkstra {

    public static class Result {
        public Map<String, Integer> distances = new HashMap<>();
        public Map<String, String> previous = new HashMap<>();
    }

    public static Result findShortestPaths(Graph g, String start) {

        Result r = new Result();

        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingInt(r.distances::get));

        for (String node : g.adj.keySet()) {
            r.distances.put(node, Integer.MAX_VALUE);
        }

        r.distances.put(start, 0);
        pq.add(start);

        while (!pq.isEmpty()) {

            String u = pq.poll();

            for (Graph.Edge e : g.adj.get(u)) {

                int newDist = r.distances.get(u) + e.weight;

                if (newDist < r.distances.get(e.to)) {

                    r.distances.put(e.to, newDist);
                    r.previous.put(e.to, u);

                    pq.add(e.to);
                }
            }
        }

        return r;
    }

    public static List<String> getPath(Map<String, String> prev, String target) {

        LinkedList<String> path = new LinkedList<>();
        String current = target;

        while (current != null) {
            path.addFirst(current);
            current = prev.get(current);
        }

        return path;
    }
}