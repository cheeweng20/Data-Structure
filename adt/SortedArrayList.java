package adt;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * SortedArrayList.java A class that implements the ADT Sorted List using an array.
 *
 * @author Frank M. Carrano
 * @version 2.0
 * @param <T>
 */
public class SortedArrayList<T extends Comparable<T>> implements SortedListInterface<T> {

  private T[] array;
  private int numberOfEntries;
  private final Comparator<? super T> comparator;
  private static final int DEFAULT_CAPACITY = 25;

  public SortedArrayList() {
    this(DEFAULT_CAPACITY, null);
  }

  public SortedArrayList(int initialCapacity) {
    this(initialCapacity, null);
  }

  public SortedArrayList(Comparator<? super T> comparator) {
    this(DEFAULT_CAPACITY, comparator);
  }

  @SuppressWarnings("unchecked")
  public SortedArrayList(int initialCapacity, Comparator<? super T> comparator) {
    if (initialCapacity < 1) {
      throw new IllegalArgumentException("Initial capacity must be greater than zero.");
    }
    numberOfEntries = 0;
    array = (T[]) new Comparable[initialCapacity];
    this.comparator = comparator;
  }

  @Override
  public boolean add(T newEntry) {
    ensureCapacity();
    int i = 0;
    while (i < numberOfEntries && compare(newEntry, array[i]) >= 0) {
      i++;
    }
    makeRoom(i + 1);
    array[i] = newEntry;
    numberOfEntries++;
    return true;
  }

  @Override
  public boolean remove(T anEntry) {
    for (int index = 0; index < numberOfEntries; index++) {
      if (anEntry.equals(array[index])) {
        removeGap(index + 1);
        numberOfEntries--;
        array[numberOfEntries] = null;
        return true;
      }
    }
    return false;
  }

  @Override
  public void clear() {
    for (int index = 0; index < numberOfEntries; index++) {
      array[index] = null;
    }
    numberOfEntries = 0;
  }

  @Override
  public boolean contains(T anEntry) {
    boolean found = false;
    for (int index = 0; !found && (index < numberOfEntries); index++) {
      if (anEntry.equals(array[index])) {
        found = true;
      }
    }
    return found;
  }

  @Override
  public T getEntry(int givenPosition) {
    if (givenPosition < 1 || givenPosition > numberOfEntries) {
      return null;
    }
    return array[givenPosition - 1];
  }

  @Override
  public int getNumberOfEntries() {
    return numberOfEntries;
  }

  @Override
  public boolean isEmpty() {
    return numberOfEntries == 0;
  }

  @Override
  public Iterator<T> iterator() {
    return new SortedArrayListIterator();
  }

  private class SortedArrayListIterator implements Iterator<T> {
    private int currentIndex;

    @Override
    public boolean hasNext() {
      return currentIndex < numberOfEntries;
    }

    @Override
    public T next() {
      if (!hasNext()) {
        throw new NoSuchElementException("No more entries in the sorted list.");
      }
      return array[currentIndex++];
    }
  }

  @Override
  public String toString() {
    StringBuilder output = new StringBuilder();
    for (int index = 0; index < numberOfEntries; ++index) {
      output.append(array[index]).append('\n');
    }
    return output.toString();
  }

  private boolean isArrayFull() {
    return numberOfEntries == array.length;
  }

  @SuppressWarnings("unchecked")
  private void doubleArray() {
    T[] oldList = array;
    int oldSize = oldList.length;

    array = (T[]) new Comparable[2 * oldSize];

    for (int index = 0; index < oldSize; index++) {
      array[index] = oldList[index];
    }
  }

  private void ensureCapacity() {
    if (isArrayFull()) {
      doubleArray();
    }
  }

  private int compare(T left, T right) {
    return comparator == null ? left.compareTo(right) : comparator.compare(left, right);
  }

  private void makeRoom(int newPosition) {
    int newIndex = newPosition - 1;
    int lastIndex = numberOfEntries - 1;

    for (int index = lastIndex; index >= newIndex; index--) {
      array[index + 1] = array[index];
    }
  }

  private void removeGap(int givenPosition) {
    int removedIndex = givenPosition - 1;
    int lastIndex = numberOfEntries - 1;

    
    for (int index = removedIndex; index < lastIndex; index++) {
      array[index] = array[index + 1];
    }
  }

}
