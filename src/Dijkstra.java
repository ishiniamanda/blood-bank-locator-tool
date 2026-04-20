
import java.util.*;

public class Dijkstra {

    
    static class Node implements Comparable<Node> {
        String name;
        int distance;

        Node(String name, int distance) {
            this.name = name;
            this.distance = distance;
        }

        @Override
        public int compareTo(Node other) {
            return Integer.compare(this.distance, other.distance);
        }
    }

    public static Map<String, Integer> findShortestPaths(Graph graphObj, String startNode) {
        Map<String, Map<String, Integer>> adjacencyList = graphObj.getGraph();
        Map<String, Integer> distances = new HashMap<>();
        PriorityQueue<Node> pq = new PriorityQueue<>();

        
        for (String node : adjacencyList.keySet()) {
            distances.put(node, Integer.MAX_VALUE);
        }
        distances.put(startNode, 0);
        pq.add(new Node(startNode, 0));

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            String u = current.name;

           
            if (adjacencyList.get(u) != null) {
                for (Map.Entry<String, Integer> neighbor : adjacencyList.get(u).entrySet()) {
                    String v = neighbor.getKey();
                    int weight = neighbor.getValue();
                    int newDist = distances.get(u) + weight;

                    // If a shorter path is found, update the record
                    if (newDist < distances.get(v)) {
                        distances.put(v, newDist);
                        pq.add(new Node(v, newDist));
                    }
                }
            }
        }
        return distances;
    }
}
