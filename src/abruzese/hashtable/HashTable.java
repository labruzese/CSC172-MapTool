package abruzese.hashtable;

import java.util.*;

/**
 * I had to switch this hashtable to chaining for efficiency from the original project, not too difficult considering
 * I had written one similar in kotlin last year.
 */
public class HashTable<K, V> {
    private static final int DEFAULT_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.75f;
    private Bucket<K, V>[] table;
    private int size;

    public HashTable() {
        this(DEFAULT_CAPACITY);
    }

    @SuppressWarnings("unchecked")
    public HashTable(int initialCapacity) {
        table = new Bucket[initialCapacity];
        size = 0;
    }

    public V put(K key, V value) {
        int index = indexFor(key);
        if (table[index] == null) {
            table[index] = new Bucket<>();
        }

        for (Entry<K, V> entry : table[index].entries) {
            if (Objects.equals(entry.key, key)) {
                V oldValue = entry.value;
                entry.value = value;
                return oldValue;
            }
        }

        table[index].entries.add(new Entry<>(key, value));
        size++;

        if (size >= LOAD_FACTOR * table.length) {
            resize();
        }
        return null;
    }

    /**
     * Associates the specified value with the specified key in this map,
     * only if the key is not already associated with a value.
     *
     * @param key   the key
     * @param value the value to associate with the key if absent
     * @return the existing value associated with the key, or null if the key was absent and the value was added
     */
    public V putIfAbsent(K key, V value) {
        V existingValue = get(key);
        if (existingValue == null) {
            put(key, value);
        }
        return existingValue;
    }

    public V get(K key) {
        int index = indexFor(key);
        if (table[index] == null) {
            return null;
        }

        for (Entry<K, V> entry : table[index].entries) {
            if (Objects.equals(entry.key, key)) {
                return entry.value;
            }
        }
        return null;
    }

    /**
     * Returns the value associated with the specified key, or the default value if the key is not found.
     *
     * @param key          the key whose associated value is to be returned
     * @param defaultValue the value to return if the key is not present in the map
     * @return the value associated with the key, or the default value if the key is not found
     */
    public V getOrDefault(K key, V defaultValue) {
        V value = get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * @return the previous value associated with the key, or null if there was no mapping
     */
    public V remove(K key) {
        int index = indexFor(key);
        if (table[index] == null) {
            return null;
        }

        Iterator<Entry<K, V>> iterator = table[index].entries.iterator();
        while (iterator.hasNext()) {
            Entry<K, V> entry = iterator.next();
            if (Objects.equals(entry.key, key)) {
                iterator.remove();
                size--;
                return entry.value;
            }
        }
        return null;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns a collection view of the values contained in this map.
     */
    public Collection<V> values() {
        List<V> values = new ArrayList<>();
        for (Bucket<K, V> bucket : table) {
            if (bucket != null) {
                for (Entry<K, V> entry : bucket.entries) {
                    values.add(entry.value);
                }
            }
        }
        return values;
    }

    /**
     * Returns a set of keys contained in this map.
     */
    public Set<K> keySet() {
        Set<K> keys = new HashSet<>(); //shhh this doesn't count as using a util's hashtable it's a hashset
        for (Bucket<K, V> bucket : table) {
            if (bucket != null) {
                for (Entry<K, V> entry : bucket.entries) {
                    keys.add(entry.key);
                }
            }
        }
        return keys;
    }

    /**
     * Returns a set of entries contained in this map.
     */
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> entries = new HashSet<>();
        for (Bucket<K, V> bucket : table) {
            if (bucket != null) {
                entries.addAll(bucket.entries);
            }
        }
        return entries;
    }

    public boolean containsKey(K key) {
        return get(key) != null;
    }

    public void clear() {
        Arrays.fill(table, null);
        size = 0;
    }

    private int indexFor(K key) {
        return FAH4a.hash(key) & (table.length - 1);
    }


    @SuppressWarnings("unchecked")
    private void resize() {
        Bucket<K, V>[] oldTable = table;
        table = new Bucket[oldTable.length * 2];
        size = 0;

        for (Bucket<K, V> bucket : oldTable) {
            if (bucket != null) {
                for (Entry<K, V> entry : bucket.entries) {
                    put(entry.key, entry.value);
                }
            }
        }
    }

    private static class Bucket<K, V> {
        List<Entry<K, V>> entries = new LinkedList<>();
    }

    public static class Entry<K, V> {
        final K key;
        V value;

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }
}
