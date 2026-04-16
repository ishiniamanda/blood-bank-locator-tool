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

        g.display();
    }
}