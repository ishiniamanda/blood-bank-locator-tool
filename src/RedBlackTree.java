import java.util.ArrayList;

public class RedBlackTree {
    private static final boolean RED = true;
    private static final boolean BLACK = false;

    class Node {
        String bloodType; // The search key
        ArrayList<Donor> donors; // The list of all hospitals with this blood type
        Node left, right, parent;
        boolean color;

        Node(Donor donor) {
            this.bloodType = donor.bloodType;
            this.donors = new ArrayList<>();
            this.donors.add(donor); // Add the first hospital
            this.color = RED;
        }
    }

    private Node root;

    // Search now returns a List of donors instead of just one
    public ArrayList<Donor> search(String bloodType) {
        Node current = root;
        while (current != null) {
            if (bloodType.equals(current.bloodType)) {
                return current.donors; // Match found! Return the whole list
            }
            if (bloodType.compareTo(current.bloodType) < 0) {
                current = current.left;
            } else {
                current = current.right;
            }
        }
        return new ArrayList<>(); // Return empty list if not found to prevent crashes
    }

    public void insert(Donor donor) {
        root = insertRec(root, donor);
        root.color = BLACK; 
    }

    private Node insertRec(Node node, Donor donor) {
        if (node == null) return new Node(donor);

        int cmp = donor.bloodType.compareTo(node.bloodType);
        
        if (cmp == 0) {
            // Collision handling: The blood type already exists in the tree!
            // We simply add this new hospital to the existing node's list.
            node.donors.add(donor);
            return node; 
        } else if (cmp < 0) {
            node.left = insertRec(node.left, donor);
            node.left.parent = node;
        } else {
            node.right = insertRec(node.right, donor);
            node.right.parent = node;
        }
        return node;
    }
}