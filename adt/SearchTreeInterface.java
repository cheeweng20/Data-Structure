package adt;

/**
 * An interface for a key-value search tree ADT.
 *
 * @param <K> the comparable key type
 * @param <V> the value type
 */
public interface SearchTreeInterface<K extends Comparable<? super K>, V> {

    /**
     * Adds a value under a key. An existing value with the same key is replaced.
     *
     * @param key the key used to locate the value
     * @param value the value to store
     */
    void add(K key, V value);

    /**
     * Retrieves the value stored under a key.
     *
     * @param key the key to find
     * @return the stored value, or {@code null} when the key is absent
     */
    V search(K key);
}
