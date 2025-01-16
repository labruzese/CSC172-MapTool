package abruzese.priorityQueue;
//Author Skylar Abruzese
public class PriorityQueue<T extends Comparable<T>>{
    private T[] heap;
    private int size;

    public int size() {
        return size;
    }

    public PriorityQueue() {
        this(10);
    }

    @SuppressWarnings("unchecked")
    public PriorityQueue(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be positive");

        heap = (T[]) new Comparable[capacity];
        size = 0;
    }

    public PriorityQueue(T[] array) {
        if (array == null) throw new IllegalArgumentException("Input array cannot be null");

        heap = (T[]) new Comparable[array.length];
        size = array.length;
        for (int i = 0; i < array.length; i++) {
            if (array[i] == null) throw new IllegalArgumentException("Array elements cannot be null");
            heap[i] = array[i];
        }
        heapify();
    }

    private void heapify() {
        for (int i = getParent(size-1); i >= 0; i--) {
            bubbleDown(i);
        }
    }

    public void enqueue(T value) {
        if (value == null) throw new IllegalArgumentException("Cannot insert null value");

        if (size == heap.length) resize();
        heap[size] = value;
        bubbleUp(size++);
    }

    public T poll() {
        if (isEmpty()) throw new IllegalStateException("Heap is empty");

        T min = heap[0];
        heap[0] = heap[--size];
        heap[size] = null;  // Help GC
        if (size > 0) {
            bubbleDown(0);
        }
        return min;
    }

    private void bubbleUp(int index) {
        int parent = getParent(index);
        if (index > 0 && heap[index].compareTo(heap[parent]) < 0) {
            swap(index, parent);
            bubbleUp(parent);
        }
    }

    private void bubbleDown(int index) {
        int left = getLeft(index);
        int right = getRight(index);
        int smallest = index;

        if (left < size && heap[left].compareTo(heap[smallest]) < 0) {
            smallest = left;
        }
        if (right < size && heap[right].compareTo(heap[smallest]) < 0) {
            smallest = right;
        }
        if (smallest != index) {
            swap(index, smallest);
            bubbleDown(smallest);
        }
    }

    @SuppressWarnings("unchecked")
    private void resize() {
        if (heap.length >= Integer.MAX_VALUE-1) throw new OutOfMemoryError("Heap at maximum capacity");

        int newSize = heap.length < Integer.MAX_VALUE/2? heap.length * 2 : Integer.MAX_VALUE-1;
        T[] newHeap = (T[]) new Comparable[newSize];
        System.arraycopy(heap, 0, newHeap, 0, size);
        heap = newHeap;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public T peek() {
        if (isEmpty()) throw new IllegalStateException("Heap is empty");
        return heap[0];
    }

    @SuppressWarnings("unchecked")
    public T[] toArray() {
        T[] arr = (T[]) new Comparable[heap.length];
        System.arraycopy(heap, 0, arr, 0, heap.length);
        return arr;
    }

    public PriorityQueue<T> copy() {
        return new PriorityQueue<>(this.toArray());
    }

    private void swap(int i, int j) {
        T temp = heap[i];
        heap[i] = heap[j];
        heap[j] = temp;
    }

    private static int getParent(int index) {
        return (index - 1) / 2;
    }

    private static int getRight(int index) {
        return 2 * index + 2;
    }

    private static int getLeft(int index) {
        return 2 * index + 1;
    }

    @Override
    public String toString() {
        if (isEmpty()) return "[]";

        StringBuilder sb = new StringBuilder();

        // Array representation
        sb.append("Array: [");
        for (int i = 0; i < heap.length; i++) {
            sb.append(heap[i]!=null? heap[i] : "_");

            if (i < heap.length - 1) sb.append(", ");
        }
        sb.append("]\n");

        // Tree representation
        for (int level = 0, firstIndex = 0; firstIndex < size; level++, firstIndex = getLeft(firstIndex)) {
            int spaces = (int) Math.pow(2, Math.max(0, 3 - level)) - 1;
            int nodesAtLevel = Math.min(size - firstIndex, (int) Math.pow(2, level));

            sb.append(" ".repeat(spaces));

            for (int i = 0; i < nodesAtLevel; i++) {
                sb.append(heap[firstIndex + i]);
                if (i < nodesAtLevel - 1) {
                    sb.append(" ".repeat(Math.max(1, spaces * 2 + 1 - String.valueOf(heap[firstIndex + i]).length())));
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
