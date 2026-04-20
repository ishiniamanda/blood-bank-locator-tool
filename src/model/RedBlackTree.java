package model;

public class RedBlackTree {

    class Node {
        String bloodType;
        String donorName;
        Node left, right;
        boolean red = true;

        Node(String bloodType, String donorName) {
            this.bloodType = bloodType;
            this.donorName = donorName;
        }
    }

    private Node root;

    // INSERT
    public void insert(String bloodType, String donorName) {
        root = insert(root, bloodType, donorName);
        root.red = false;
    }

    private Node insert(Node h, String bt, String name) {

        if (h == null) return new Node(bt, name);

        if (bt.compareTo(h.bloodType) < 0)
            h.left = insert(h.left, bt, name);
        else
            h.right = insert(h.right, bt, name);

        return h;
    }

    // SEARCH
    public String search(String bloodType) {
        Node current = root;

        while (current != null) {
            if (bloodType.equals(current.bloodType))
                return current.donorName;

            if (bloodType.compareTo(current.bloodType) < 0)
                current = current.left;
            else
                current = current.right;
        }

        return "No donor found";
    }
}