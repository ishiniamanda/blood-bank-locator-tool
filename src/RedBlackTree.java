import java.util.ArrayList;

public class RedBlackTree {

    private static final boolean RED = true;
    private static final boolean BLACK = false;

    class Node {
        String bloodType;
        ArrayList<Donor> donors;
        Node left, right, parent;
        boolean color;

        Node(Donor donor) {
            this.bloodType = donor.getBloodType();
            this.donors = new ArrayList<>();
            this.donors.add(donor);
            this.color = RED;
        }
    }

    private Node root;

    // ================= SEARCH =================
    public ArrayList<Donor> search(String bloodType) {
        Node current = root;

        while (current != null) {
            int cmp = bloodType.compareTo(current.bloodType);

            if (cmp == 0)
                return current.donors;

            else if (cmp < 0)
                current = current.left;

            else
                current = current.right;
        }

        return new ArrayList<>();
    }

    // ================= INSERT =================
    public void insert(Donor donor) {
        Node newNode = new Node(donor);
        root = bstInsert(root, newNode);

        if (newNode.parent == null) {
            newNode.color = BLACK;
            return;
        }

        fixInsert(newNode);
    }

    private Node bstInsert(Node root, Node node) {

        if (root == null)
            return node;

        int cmp = node.bloodType.compareTo(root.bloodType);

        if (cmp == 0) {
            root.donors.add(node.donors.get(0));
            return root;
        }

        else if (cmp < 0) {
            root.left = bstInsert(root.left, node);
            root.left.parent = root;
        }

        else {
            root.right = bstInsert(root.right, node);
            root.right.parent = root;
        }

        return root;
    }

    // ================= FIX VIOLATIONS =================
    private void fixInsert(Node k) {

        while (k != root && k.parent != null && k.parent.color == RED) {

            Node parent = k.parent;
            Node grandparent = parent.parent;

            // 🔴 SAFETY FIX (IMPORTANT)
            if (grandparent == null) {
                break;
            }

            if (parent == grandparent.left) {

                Node uncle = grandparent.right;

                // Case 1: Uncle is RED
                if (uncle != null && uncle.color == RED) {
                    parent.color = BLACK;
                    uncle.color = BLACK;
                    grandparent.color = RED;
                    k = grandparent;
                }

                else {
                    // Case 2
                    if (k == parent.right) {
                        k = parent;
                        rotateLeft(k);
                    }

                    // Case 3
                    parent.color = BLACK;
                    grandparent.color = RED;
                    rotateRight(grandparent);
                }

            } else {

                Node uncle = grandparent.left;

                // Case 1
                if (uncle != null && uncle.color == RED) {
                    parent.color = BLACK;
                    uncle.color = BLACK;
                    grandparent.color = RED;
                    k = grandparent;
                }

                else {
                    // Case 2
                    if (k == parent.left) {
                        k = parent;
                        rotateRight(k);
                    }

                    // Case 3
                    parent.color = BLACK;
                    grandparent.color = RED;
                    rotateLeft(grandparent);
                }
            }
        }

        if (root != null)
            root.color = BLACK;
    }

    // ================= ROTATIONS =================
    private void rotateLeft(Node x) {

        Node y = x.right;
        if (y == null) return;   // 🔴 FIX ADDED

        x.right = y.left;

        if (y.left != null)
            y.left.parent = x;

        y.parent = x.parent;

        if (x.parent == null)
            root = y;

        else if (x == x.parent.left)
            x.parent.left = y;

        else
            x.parent.right = y;

        y.left = x;
        x.parent = y;
    }

    private void rotateRight(Node x) {

        Node y = x.left;
        if (y == null) return;   // 🔴 FIX ADDED

        x.left = y.right;

        if (y.right != null)
            y.right.parent = x;

        y.parent = x.parent;

        if (x.parent == null)
            root = y;

        else if (x == x.parent.right)
            x.parent.right = y;

        else
            x.parent.left = y;

        y.right = x;
        x.parent = y;
    }
}