public class RedBlackTree {
    private static final boolean RED = true;
    private static final boolean BLACK = false;

    class Node {
        Donor donor;
        Node left, right, parent;
        boolean color;

        Node(Donor donor) {
            this.donor = donor;
            this.color = RED;
        }
    }

    private Node root;

   
    public Donor search(String bloodType) {
        Node current = root;
        while (current != null) {
            if (bloodType.equals(current.donor.bloodType)) {
                return current.donor;
            }
            if (bloodType.compareTo(current.donor.bloodType) < 0) {
                current = current.left;
            } else {
                current = current.right;
            }
        }
        return null;
    }

    // Simplified Insert for your project
    public void insert(Donor donor) {
        root = insertRec(root, donor);
        root.color = BLACK; 
    }

    private Node insertRec(Node node, Donor donor) {
        if (node == null) return new Node(donor);

        if (donor.bloodType.compareTo(node.donor.bloodType) < 0) {
            node.left = insertRec(node.left, donor);
            node.left.parent = node;
        } else {
            node.right = insertRec(node.right, donor);
            node.right.parent = node;
        }
        return node;
    }
}