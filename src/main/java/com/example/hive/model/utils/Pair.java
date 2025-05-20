package com.example.hive.model.utils;

/**
 * A simple generic class to hold a key-value pair.
 * This class is useful for representing a pair of related objects.
 *
 * @param <K> the type of the key.
 * @param <V> the type of the value.
 */
public class Pair<K, V> {
    private final K key;
    private final V value;

    /**
     * Constructs a new Pair with the specified key and value.
     *
     * @param key the key for the pair.
     * @param value the value for the pair.
     */
    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Constructs a new Pair with null key and value.
     * This constructor is provided for convenience when you want an empty pair.
     */
    public Pair(){
        key = null;
        value = null;
    }

    /**
     * Gets the key of this Pair.
     *
     * @return the key of the pair.
     */
    public K getKey() {
        return key;
    }

    /**
     * Gets the value of this Pair.
     *
     * @return the value of the pair.
     */
    public V getValue() {
        return value;
    }

    /**
     * Compares this Pair to another object for equality.
     * Two pairs are considered equal if they have the same key and value.
     *
     * @param obj the object to compare to.
     * @return true if the objects are equal, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) obj;
        return key.equals(pair.key) && value.equals(pair.value);
    }

    /**
     * Returns the hash code for this Pair, calculated from the key and value.
     *
     * @return the hash code of this Pair.
     */
    @Override
    public int hashCode() {
        return key.hashCode() ^ value.hashCode();
    }

    /**
     * Returns a string representation of this Pair in the form (key, value).
     *
     * @return a string representation of the Pair.
     */
    @Override
    public String toString() {
        return "(" + key + ", " + value + ")";
    }
}
