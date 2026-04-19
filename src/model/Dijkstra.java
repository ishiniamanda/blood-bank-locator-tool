package model;

import java.util.*;

public class Dijkstra {

    public static class Result {
        public Map<String, Integer> distances;
        public Map<String, String> previous;

        public Result(Map<String, Integer> d, Map<String, String> p) {
            this.distances = d;
            this.previous = p;
        }
    }

    public static Result findShortestPaths(Graph g, String start) {

        Map<String, Integer> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();

        PriorityQueue<String> pq =
                new PriorityQueue<>(Comparator.comparingInt(dist::get));

        Set<String> visited = new HashSet<>();

        for (String node : g.getAdj().keySet()) {
            dist.put(node, Integer.MAX_VALUE);
            prev.put(node, null);
        }

        if (!dist.containsKey(start)) {
            System.out.println("Start node not found in graph!");
            return new Result(dist, prev);
        }

        dist.put(start, 0);
        pq.add(start);

        while (!pq.isEmpty()) {

            String current = pq.poll();

            if (visited.contains(current)) continue;
            visited.add(current);

            List<Graph.Edge> neighbors =
                    g.getAdj().getOrDefault(current, new ArrayList<>());

            for (Graph.Edge edge : neighbors) {

                String next = edge.node;
                int newDist = dist.get(current) + edge.weight;

                if (newDist < dist.get(next)) {
                    dist.put(next, newDist);
                    prev.put(next, current);
                    pq.add(next);
                }
            }
        }

        return new Result(dist, prev);
    }

    public static List<String> getPath(Map<String, String> prev, String target) {

        List<String> path = new ArrayList<>();

        if (!prev.containsKey(target)) return path;

        for (String at = target; at != null; at = prev.get(at)) {
            path.add(at);
        }

        Collections.reverse(path);
        return path;
    }
}