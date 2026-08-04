package FrontDeskService.adt;

/**
 * Binary-search-tree ADT used by the Front Desk Service.
 * The key is an eight-digit confirmation number.
 *
 * @author Front Desk Service team
 */
public class ConfirmationSearchTree<T> {
    private Node root;

    public void add(String key, T value) {
        root = add(root, key, value);
    }

    public T search(String key) {
        Node current = root;
        while (current != null) {
            int comparison = key.compareTo(current.key);
            if (comparison == 0) {
                return current.value;
            }
            current = comparison < 0 ? current.left : current.right;
        }
        return null;
    }

    private Node add(Node node, String key, T value) {
        if (node == null) {
            return new Node(key, value);
        }
        int comparison = key.compareTo(node.key);
        if (comparison < 0) {
            node.left = add(node.left, key, value);
        } else if (comparison > 0) {
            node.right = add(node.right, key, value);
        } else {
            node.value = value;
        }
        return node;
    }

    private class Node {
        private final String key;
        private T value;
        private Node left;
        private Node right;

        private Node(String key, T value) {
            this.key = key;
            this.value = value;
        }
    }
}
