package abruzese.priorityQueue;

import java.util.Collection;
import java.util.Comparator;
import java.util.NoSuchElementException;

/**
 * A priority queue that also accepts a comparator.
 */
public class PriorityQueue<T> {
    private T[] heap;
    private int size;
    private final Comparator<? super T> comparator;
    private static final int DEFAULT_INITIAL_CAPACITY = 10;
    private static final double GROWTH_FACTOR = 1.5;

    @SuppressWarnings("unchecked")
    public PriorityQueue(Comparator<? super T> comparator, int initialCapacity) {
        if (initialCapacity <= 0) throw new IllegalArgumentException("Initial capacity must be positive");
        if (comparator == null) throw new IllegalArgumentException("Comparator cannot be null");

        this.comparator = comparator;
        this.heap = (T[]) new Object[initialCapacity];
        this.size = 0;
    }

    public PriorityQueue(Comparator<? super T> comparator) {
        this(comparator, DEFAULT_INITIAL_CAPACITY);
    }

    @SuppressWarnings("unchecked")
    public PriorityQueue(Collection<? extends T> collection, Comparator<? super T> comparator) {
        if (collection == null) throw new IllegalArgumentException("Collection cannot be null");
        if (comparator == null) throw new IllegalArgumentException("Comparator cannot be null");

        this.comparator = comparator;
        this.heap = (T[]) new Object[Math.max(collection.size(), DEFAULT_INITIAL_CAPACITY)];
        this.addAll(collection);
    }

    public void add(T element) {
        if (element == null) throw new IllegalArgumentException("Element cannot be null");

        ensureCapacity(size + 1);
        heap[size] = element;
        bubbleUp(size++);
    }

    public void addAll(Collection<? extends T> collection) {
        if (collection == null) throw new IllegalArgumentException("Collection cannot be null");

        ensureCapacity(size + collection.size());

        //first add all to the end
        for (T element : collection) {
            if (element == null) throw new IllegalArgumentException("Collection contains null element");
            heap[size++] = element;
        }

        //then heapify
        heapify();
    }

    public T poll() {
        if (isEmpty()) throw new NoSuchElementException("Priority queue is empty");

        T result = heap[0];
        heap[0] = heap[--size];
        heap[size] = null;  //help gc

        if (!isEmpty()) {
            bubbleDown(0);
        }

        return result;
    }

    public T peek() {
        if (isEmpty()) throw new NoSuchElementException("Priority queue is empty");
        return heap[0];
    }

    private void bubbleUp(int index) {
        T element = heap[index];

        while (index > 0) {
            int parentIndex = (index - 1) >>> 1;
            T parent = heap[parentIndex];

            if (comparator.compare(element, parent) >= 0) break;

            heap[index] = parent;
            index = parentIndex;
        }

        heap[index] = element;
    }

    private void bubbleDown(int index) {
        T element = heap[index];
        int halfSize = size >>> 1;

        while (index < halfSize) {
            int leftIndex = (index << 1) + 1;
            int rightIndex = leftIndex + 1;
            T left = heap[leftIndex];

            if (rightIndex < size) {
                T right = heap[rightIndex];
                if (comparator.compare(right, left) < 0) {
                    leftIndex = rightIndex;
                    left = right;
                }
            }

            if (comparator.compare(element, left) <= 0) break;

            heap[index] = left;
            index = leftIndex;
        }

        heap[index] = element;
    }

    private void heapify() {
        for (int i = (size >>> 1) - 1; i >= 0; i--) {
            bubbleDown(i);
        }
    }

    @SuppressWarnings("unchecked")
    private void ensureCapacity(int minCapacity) {
        if (minCapacity > heap.length) {
            int oldCapacity = heap.length;
            int newCapacity = oldCapacity < Integer.MAX_VALUE / 2 ?
                    (int)(oldCapacity * GROWTH_FACTOR) : Integer.MAX_VALUE;

            if (newCapacity < minCapacity) {
                newCapacity = minCapacity;
            }

            T[] newHeap = (T[]) new Object[newCapacity];
            System.arraycopy(heap, 0, newHeap, 0, size);
            heap = newHeap;
        }
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    public void clear() {
        for (int i = 0; i < size; i++) {
            heap[i] = null;
        }
        size = 0;
    }

    @SuppressWarnings("unchecked")
    public T[] toArray() {
        T[] result = (T[]) new Object[size];
        System.arraycopy(heap, 0, result, 0, size);
        return result;
    }

    @Override
    public String toString() {
        if (isEmpty()) return "[]";

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < size; i++) {
            sb.append(heap[i]);
            if (i < size - 1) {
                sb.append(", ");
            }
        }
        return sb.append("]").toString();
    }

    //this was for debugging
    public String toTreeString() {
        if (isEmpty()) return "[]";

        StringBuilder sb = new StringBuilder();
        for (int level = 0, firstIndex = 0; firstIndex < size; level++) {
            int nodesAtLevel = Math.min(size - firstIndex, 1 << level);
            int spaces = (1 << (Math.max(0, 3 - level))) - 1;

            sb.append(" ".repeat(spaces));

            for (int i = 0; i < nodesAtLevel; i++) {
                sb.append(heap[firstIndex + i]);
                if (i < nodesAtLevel - 1) {
                    sb.append(" ".repeat(Math.max(1, spaces * 2 + 1 -
                            String.valueOf(heap[firstIndex + i]).length())));
                }
            }
            sb.append('\n');
            firstIndex += nodesAtLevel;
        }
        return sb.toString();
    }

    public boolean removeIf(java.util.function.Predicate<? super T> filter) {
        if (filter == null) throw new IllegalArgumentException("Predicate cannot be null");

        boolean removed = false;
        // Create a new array to store non-removed elements
        @SuppressWarnings("unchecked")
        T[] newHeap = (T[]) new Object[heap.length];
        int newSize = 0;

        // Copy elements that don't match the predicate
        for (int i = 0; i < size; i++) {
            if (!filter.test(heap[i])) {
                newHeap[newSize++] = heap[i];
            } else {
                removed = true;
            }
        }

        // If nothing was removed, return early
        if (!removed) {
            return false;
        }

        // Update size and heap
        size = newSize;
        heap = newHeap;

        // Reheapify from scratch since removing elements can break heap property
        heapify();
        return true;
    }
}