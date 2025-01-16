package abruzese.hashtable;


/**
 * A linear probing implimentation of a UR_HashTable
 * @param <K> The key type
 * @param <V> The value type
 * @author Skylar Abruzese
 */
public class HashTable<K,V> {
    private static final int INIT_CAPACITY = 16 ;
    private static final double MAX_INSERTS_PROPORTION = 0.4; // must be <1
    private static final double OPTIMAL_PERCENT_FULL = 0.1;

    protected boolean[] graveyard; //true if there is a tombstone

    protected int n; // size of the data set
    protected int m; // size of the hash table
    protected K[] keys;
    V[] vals;
    int inserts, collisions;

    @SuppressWarnings("unchecked")
    public HashTable() {
        keys = (K[])new Object[INIT_CAPACITY];
        vals =  (V[])new Object[INIT_CAPACITY];
        graveyard = new boolean[INIT_CAPACITY];

        n = 0;
        m = keys.length;
        inserts = 0;
        collisions = 0;
    }

    @SuppressWarnings("unchecked")
    public HashTable(int cap) {
        cap = Math.max(1, cap);
        keys = (K[])new Object[cap];
        vals = (V[])new Object[cap];
        graveyard = new boolean[cap];

        n = 0;
        m = keys.length;
        inserts = 0;
        collisions = 0;
    }

    public void put(K key, V val) {
        if(key == null) throw new IllegalArgumentException("Can't add a null key");

        int i = hash(key);
        while(keys[i] != null) {
            i = (i+1) % keys.length;
        }

        keys[i] = key;
        vals[i] = val;
        graveyard[i] = true; //mark the location for probing

        n++;
        if(i != hash(key)) collisions++;
        inserts++;
        if(inserts > keys.length * MAX_INSERTS_PROPORTION) resize((int)((1/OPTIMAL_PERCENT_FULL)*n));
    }

    //Note that you can't tell the difference between an inserted object of null and one that doesn't exist. Use contains
    //to see if the object exists and is null vs doesn't exist.
    public V get(K key) {
        int location = probe(key);
        if(location == -1) return null;
        return vals[location];
    }

    public V remove(K key) {
        int i = probe(key);
        if(i == -1) return null;

        keys[i] = null;
        V val = vals[i];
        vals[i] = null;

        n--;
        return val;
    }

    public boolean contains(K key) {
        if(key == null) return false;
        return probe(key) != -1;
    }

    // Useful helpers
    protected int hash(K key) {
        return Math.floorMod(FAH4a.hash(key), keys.length);
    }

    @SuppressWarnings("unchecked")
    private void resize(int capacity) {
        K[] oldKeys = keys;
        V[] oldVals = vals;

        keys = (K[])new Object[capacity];
        vals = (V[])new Object[capacity];
        graveyard = new boolean[capacity];

        m = capacity;
        collisions = 0;
        inserts = 0;

        for (int i = 0; i < oldKeys.length; i++) {
            if(oldKeys[i] != null) put(oldKeys[i], oldVals[i]);
        }
    }

    private int probe(K key) {
        if(key == null) return -1;

        int i = hash(key);
        while(graveyard[i]) {
            if(keys[i].equals(key)) return i;

            i = (i+1) % keys.length;
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    public void clear() {
        keys = (K[])new Object[m];
        vals = (V[])new Object[m];
        graveyard = new boolean[m];
        n = 0;
    }
}

