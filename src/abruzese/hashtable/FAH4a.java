package abruzese.hashtable;

import java.nio.ByteBuffer;
import java.util.Random;

/**
 This is an algorithm written by me and my friend last year.

 This algorithm should have the following properties.

 Collision rate about equivalent to randomly generated numbers.
 Fast computation in O(1)
 Follows the object equivalence properties of key.hashcode()
 */
public class FAH4a {
    protected static final long seed = 690;
    private static byte[][][] T;

    //Provides lazy evaluation of T
    private static byte[][][] getT() {
        if(T != null) return T;

        Random gen = new Random(seed);
        T = new byte[1 << 8][1 << 8][4]; //256x256

        for (byte[][] row : T) {
            for (byte[] num : row) {
                gen.nextBytes(num);
            }
        }

        return T;
    }

    public static int hash(Object key) {
        byte[] h = new byte[4]; //This is the size of an int

        byte[] chunks = ByteBuffer.allocate(4) //size of the int that we're hashing
                .putInt(key.hashCode())
                .array();

        for(int i = 0; i < chunks.length; i+=2) {
            byte x = chunks[i];
            byte y = chunks[i+1];

            //This maintains the data from the previous chunks
            x ^= h[0];
            y ^= h[1];

            byte[] tval = getT()[y & 0xff][x & 0xff]; //ensure positive numbers

            //xor the two ints together
            for(int c = 0; c < 4; c++)
                h[c] ^= tval[c];
        }

        return ByteBuffer.wrap(h).getInt();
    }

    /**
     * Forgets the table used for this hashing algorithm and requests the memory to be freed.
     * Calling Hashtable.FAH4a.hash() after this will rebuild the table.
     */
    public static void closeTable() {
        T = null;
        System.gc();
    }
}