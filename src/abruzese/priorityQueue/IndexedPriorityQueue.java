package abruzese.priorityQueue;

import abruzese.hashtable.HashTable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


/**
 * This was added to make dijkstra's algorithm a little faster since we can look up items directly.
 */
public class IndexedPriorityQueue<E> {
    private final List<E> elements;
    private final HashTable<E, Integer> indices;
    private final Comparator<? super E> comparator;
    private int size;

    public IndexedPriorityQueue(Comparator<? super E> comparator) {
        this.elements = new ArrayList<>();
        this.indices = new HashTable<>();
        this.comparator = comparator;
        this.size = 0;
    }

    public void add(E element) {
        elements.add(element);
        indices.put(element, size);
        bubbleUp(size++);
    }

    public E poll() {
        if (size == 0) return null;
        E result = elements.getFirst();
        indices.remove(result);

        if (--size > 0) {
            E last = elements.remove(size);
            elements.set(0, last);
            indices.put(last, 0);
            bubbleDown(0);
        } else {
            elements.removeFirst();
        }
        return result;
    }

    /**
     * Decreases the priority of an element finding it using a hashtable instead of linear searching
     */
    public void decreaseKey(E element) {
        Integer index = indices.get(element);
        if (index != null) {
            bubbleUp(index);
        }
    }

    private void bubbleUp(int k) {
        E element = elements.get(k);
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            E parentElement = elements.get(parent);
            if (comparator.compare(element, parentElement) >= 0) break;
            elements.set(k, parentElement);
            indices.put(parentElement, k);
            k = parent;
        }
        elements.set(k, element);
        indices.put(element, k);
    }

    private void bubbleDown(int k) {
        int half = size >>> 1;
        E element = elements.get(k);
        while (k < half) {
            int child = (k << 1) + 1;
            E childElement = elements.get(child);
            int right = child + 1;

            if (right < size) {
                E rightElement = elements.get(right);
                if (comparator.compare(childElement, rightElement) > 0) {
                    child = right;
                    childElement = rightElement;
                }
            }

            if (comparator.compare(element, childElement) <= 0) break;

            elements.set(k, childElement);
            indices.put(childElement, k);
            k = child;
        }
        elements.set(k, element);
        indices.put(element, k);
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean contains(E element) {
        return indices.containsKey(element);
    }
}
