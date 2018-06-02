package sample.ldpc.math;

import org.apache.commons.math3.util.FastMath;

import java.io.Serializable;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

public class OpenIntToBoolHashMap implements Serializable {

    protected static final byte FREE = 0;
    protected static final byte FULL = 1;
    protected static final byte REMOVED = 2;
    private static final float LOAD_FACTOR = 0.5f;
    private static final int DEFAULT_EXPECTED_SIZE = 16;
    private static final int RESIZE_MULTIPLIER = 2;
    private static final int PERTURB_SHIFT = 5;

    private int[] keys;
    private boolean[] values;
    private byte[] states;
    private int size;
    private int mask;
    private transient int count;


    public OpenIntToBoolHashMap() {
        this(DEFAULT_EXPECTED_SIZE);
    }


    public OpenIntToBoolHashMap(final int expectedSize) {
        final int capacity = computeCapacity(expectedSize);
        keys = new int[capacity];
        values = new boolean[capacity];
        states = new byte[capacity];
        mask = capacity - 1;
    }


    private static int computeCapacity(final int expectedSize) {
        if (expectedSize == 0) {
            return 1;
        }
        final int capacity = (int) FastMath.ceil(expectedSize / LOAD_FACTOR);
        final int powerOfTwo = Integer.highestOneBit(capacity);
        if (powerOfTwo == capacity) {
            return capacity;
        }
        return nextPowerOfTwo(capacity);
    }


    private static int nextPowerOfTwo(final int i) {
        return Integer.highestOneBit(i) << 1;
    }


    public boolean get(final int key) {

        final int hash = hashOf(key);
        int index = hash & mask;
        if (containsKey(key, index)) {
            return values[index];
        }

        if (states[index] == FREE) {
            return false;
        }

        int j = index;
        for (int perturb = perturb(hash); states[index] != FREE; perturb >>= PERTURB_SHIFT) {
            j = probe(perturb, j);
            index = j & mask;
            if (containsKey(key, index)) {
                return values[index];
            }
        }

        return false;

    }


    public boolean containsKey(final int key) {

        final int hash = hashOf(key);
        int index = hash & mask;
        if (containsKey(key, index)) {
            return true;
        }

        if (states[index] == FREE) {
            return false;
        }

        int j = index;
        for (int perturb = perturb(hash); states[index] != FREE; perturb >>= PERTURB_SHIFT) {
            j = probe(perturb, j);
            index = j & mask;
            if (containsKey(key, index)) {
                return true;
            }
        }

        return false;

    }


    public Iterator iterator() {
        return new Iterator();
    }


    private static int perturb(final int hash) {
        return hash & 0x7fffffff;
    }


    private int findInsertionIndex(final int key) {
        return findInsertionIndex(keys, states, key, mask);
    }


    private static int findInsertionIndex(final int[] keys, final byte[] states,
                                          final int key, final int mask) {
        final int hash = hashOf(key);
        int index = hash & mask;
        if (states[index] == FREE) {
            return index;
        } else if (states[index] == FULL && keys[index] == key) {
            return changeIndexSign(index);
        }

        int perturb = perturb(hash);
        int j = index;
        if (states[index] == FULL) {
            while (true) {
                j = probe(perturb, j);
                index = j & mask;
                perturb >>= PERTURB_SHIFT;

                if (states[index] != FULL || keys[index] == key) {
                    break;
                }
            }
        }

        if (states[index] == FREE) {
            return index;
        } else if (states[index] == FULL) {
            return changeIndexSign(index);
        }

        final int firstRemoved = index;
        while (true) {
            j = probe(perturb, j);
            index = j & mask;

            if (states[index] == FREE) {
                return firstRemoved;
            } else if (states[index] == FULL && keys[index] == key) {
                return changeIndexSign(index);
            }

            perturb >>= PERTURB_SHIFT;

        }

    }


    private static int probe(final int perturb, final int j) {
        return (j << 2) + j + perturb + 1;
    }


    private static int changeIndexSign(final int index) {
        return -index - 1;
    }


    public int size() {
        return size;
    }


    public boolean remove(final int key) {

        final int hash  = hashOf(key);
        int index = hash & mask;
        if (containsKey(key, index)) {
            return doRemove(index);
        }

        if (states[index] == FREE) {
            return false;
        }

        int j = index;
        for (int perturb = perturb(hash); states[index] != FREE; perturb >>= PERTURB_SHIFT) {
            j = probe(perturb, j);
            index = j & mask;
            if (containsKey(key, index)) {
                return doRemove(index);
            }
        }

        return false;

    }


    private boolean containsKey(final int key, final int index) {
        return (key != 0 || states[index] == FULL) && keys[index] == key;
    }


    private boolean doRemove(int index) {
        keys[index] = 0;
        states[index] = REMOVED;
        final boolean previous = values[index];
        values[index] = false;
        --size;
        ++count;
        return previous;
    }


    public boolean put(final int key, final boolean value) {
        int index = findInsertionIndex(key);
        boolean previous = false;
        boolean newMapping = true;
        if (index < 0) {
            index = changeIndexSign(index);
            previous = values[index];
            newMapping = false;
        }
        keys[index] = key;
        states[index] = FULL;
        values[index] = value;
        if (newMapping) {
            ++size;
            if (shouldGrowTable()) {
                growTable();
            }
            ++count;
        }
        return previous;

    }


    private void growTable() {

        final int oldLength = states.length;
        final int[] oldKeys = keys;
        final boolean[] oldValues = values;
        final byte[] oldStates = states;

        final int newLength = RESIZE_MULTIPLIER * oldLength;
        final int[] newKeys = new int[newLength];
        final boolean[] newValues = new boolean[newLength];
        final byte[] newStates = new byte[newLength];
        final int newMask = newLength - 1;
        for (int i = 0; i < oldLength; ++i) {
            if (oldStates[i] == FULL) {
                final int key = oldKeys[i];
                final int index = findInsertionIndex(newKeys, newStates, key, newMask);
                newKeys[index]   = key;
                newValues[index] = oldValues[i];
                newStates[index] = FULL;
            }
        }

        mask   = newMask;
        keys   = newKeys;
        values = newValues;
        states = newStates;

    }


    private boolean shouldGrowTable() {
        return size > (mask + 1) * LOAD_FACTOR;
    }


    private static int hashOf(final int key) {
        final int h = key ^ ((key >>> 20) ^ (key >>> 12));
        return h ^ (h >>> 7) ^ (h >>> 4);
    }


    public class Iterator {

        private final int referenceCount;
        private int current;
        private int next;


        private Iterator() {
            referenceCount = count;
            next = -1;
            try {
                advance();
            } catch (NoSuchElementException nsee) {}
        }


        public boolean hasNext() {
            return next >= 0;
        }


        public int key()
                throws ConcurrentModificationException, NoSuchElementException {
            if (referenceCount != count) {
                throw new ConcurrentModificationException();
            }
            if (current < 0) {
                throw new NoSuchElementException();
            }
            return keys[current];
        }


        public boolean value()
                throws ConcurrentModificationException, NoSuchElementException {
            if (referenceCount != count) {
                throw new ConcurrentModificationException();
            }
            if (current < 0) {
                throw new NoSuchElementException();
            }
            return values[current];
        }


        public void advance()
                throws ConcurrentModificationException, NoSuchElementException {

            if (referenceCount != count) {
                throw new ConcurrentModificationException();
            }

            current = next;

            try {
                while (states[++next] != FULL) { }
            } catch (ArrayIndexOutOfBoundsException e) {
                next = -2;
                if (current < 0) {
                    throw new NoSuchElementException();
                }
            }

        }

    }

}
