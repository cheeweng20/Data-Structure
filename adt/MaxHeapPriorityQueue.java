package adt;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class MaxHeapPriorityQueue<T extends Comparable<T>> implements PriorityQueueInterface<T> {

  private T[] heap;
  private int numberOfEntries;
  private final Comparator<? super T> comparator;
  private static final int DEFAULT_CAPACITY = 25;

  public MaxHeapPriorityQueue() {
    this(DEFAULT_CAPACITY, null);
  }

  public MaxHeapPriorityQueue(Comparator<? super T> comparator) {
    this(DEFAULT_CAPACITY, comparator);
  }

  @SuppressWarnings("unchecked")
  public MaxHeapPriorityQueue(int initialCapacity, Comparator<? super T> comparator) {
    if (initialCapacity < 1) {
      throw new IllegalArgumentException("Initial capacity must be greater than zero.");
    }
    heap = (T[]) new Object[initialCapacity + 1];
    numberOfEntries = 0;
    this.comparator = comparator;
  }

  @Override
  public void enqueue(T newEntry) {
    ensureCapacity();
    heap[++numberOfEntries] = newEntry;
    reheapUp(numberOfEntries);
  }

  @Override
  public T dequeue() {
    if (isEmpty()) {
      return null;
    }

    T front = heap[1];
    heap[1] = heap[numberOfEntries];
    heap[numberOfEntries] = null;
    numberOfEntries--;
    reheapDown(1);
    return front;
  }

  @Override
  public T getFront() {
    return isEmpty() ? null : heap[1];
  }

  @Override
  public boolean isEmpty() {
    return numberOfEntries == 0;
  }

  @Override
  public void clear() {
    for (int i = 1; i <= numberOfEntries; i++) {
      heap[i] = null;
    }
    numberOfEntries = 0;
  }

  @Override
  public int getNumberOfEntries() {
    return numberOfEntries;
  }

  @Override
  public Iterator<T> getIterator() {
    return new PriorityIterator();
  }

  private void reheapUp(int childIndex) {
    int parentIndex = childIndex / 2;

    while (childIndex > 1 && compare(heap[childIndex], heap[parentIndex]) > 0) {
      swap(childIndex, parentIndex);
      childIndex = parentIndex;
      parentIndex = childIndex / 2;
    }
  }

  private void reheapDown(int rootIndex) {
    boolean done = false;

    while (!done) {
      int leftChildIndex = rootIndex * 2;
      int rightChildIndex = leftChildIndex + 1;
      int largerChildIndex = leftChildIndex;

      if (leftChildIndex > numberOfEntries) {
        done = true;
      } else {
        if (rightChildIndex <= numberOfEntries
            && compare(heap[rightChildIndex], heap[leftChildIndex]) > 0) {
          largerChildIndex = rightChildIndex;
        }

        if (compare(heap[largerChildIndex], heap[rootIndex]) > 0) {
          swap(rootIndex, largerChildIndex);
          rootIndex = largerChildIndex;
        } else {
          done = true;
        }
      }
    }
  }

  private void ensureCapacity() {
    if (numberOfEntries == heap.length - 1) {
      doubleArray();
    }
  }

  @SuppressWarnings("unchecked")
  private void doubleArray() {
    T[] oldHeap = heap;
    heap = (T[]) new Object[oldHeap.length * 2];

    for (int i = 1; i < oldHeap.length; i++) {
      heap[i] = oldHeap[i];
    }
  }

  private int compare(T left, T right) {
    return comparator == null ? left.compareTo(right) : comparator.compare(left, right);
  }

  private void swap(int firstIndex, int secondIndex) {
    T temporary = heap[firstIndex];
    heap[firstIndex] = heap[secondIndex];
    heap[secondIndex] = temporary;
  }

  private class PriorityIterator implements Iterator<T> {

    private final MaxHeapPriorityQueue<T> snapshot;

    private PriorityIterator() {
      snapshot = new MaxHeapPriorityQueue<T>(Math.max(1, numberOfEntries), comparator);
      for (int i = 1; i <= numberOfEntries; i++) {
        snapshot.enqueue(heap[i]);
      }
    }

    @Override
    public boolean hasNext() {
      return !snapshot.isEmpty();
    }

    @Override
    public T next() {
      if (!hasNext()) {
        throw new NoSuchElementException("No more entries in the priority queue.");
      }
      return snapshot.dequeue();
    }
  }
}
