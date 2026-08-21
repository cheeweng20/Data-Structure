package adt;

/**
 * A binary-search-tree implementation of a key-value search tree ADT.
 *
 * @param <K> the comparable key type
 * @param <V> the value type
 */
public class BinarySearchTree<K extends Comparable<? super K>, V>
        implements SearchTreeInterface<K, V> {

    private Node<K, V> root;

    @Override
    public void add(K key, V value) {
        root = add(root, key, value);
    }

    @Override
    public V search(K key) {
        Node<K, V> current = root;
        while (current != null) {
            int comparison = key.compareTo(current.key);
            if (comparison == 0) {
                return current.value;
            }
            current = comparison < 0 ? current.left : current.right;
        }
        return null;
    }

    private Node<K, V> add(Node<K, V> node, K key, V value) {
        if (node == null) {
            return new Node<>(key, value);
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

    private static class Node<K, V> {
        private final K key;
        private V value;
        private Node<K, V> left;
        private Node<K, V> right;

        private Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }
}
