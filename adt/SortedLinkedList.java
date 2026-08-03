package adt;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class SortedLinkedList<T extends Comparable<T>> implements SortedListInterface<T> {

  private Node firstNode;
  private int numberOfEntries;

  public SortedLinkedList() {
    firstNode = null;
    numberOfEntries = 0;
  }

  @Override
  public boolean add(T newEntry) {
    Node newNode = new Node(newEntry);

    Node nodeBefore = null;
    Node currentNode = firstNode;
    while (currentNode != null && newEntry.compareTo(currentNode.data) >= 0) {
      nodeBefore = currentNode;
      currentNode = currentNode.next;
    }

    if (isEmpty() || (nodeBefore == null)) { // CASE 1: add at beginning
      newNode.next = firstNode;
      firstNode = newNode;
    } else {	// CASE 2: add in the middle or at the end, i.e. after nodeBefore
      newNode.next = currentNode;
      nodeBefore.next = newNode;
    }
    numberOfEntries++;
    return true;
  }

  @Override
  public boolean remove(T anEntry) {
    Node previousNode = null;
    Node currentNode = firstNode;

    while (currentNode != null && !anEntry.equals(currentNode.data)) {
      previousNode = currentNode;
      currentNode = currentNode.next;
    }

    if (currentNode == null) {
      return false;
    }

    if (previousNode == null) {
      firstNode = currentNode.next;
    } else {
      previousNode.next = currentNode.next;
    }
    numberOfEntries--;
    return true;
  }

  @Override
  public T getEntry(int givenPosition) {
    if (givenPosition < 1 || givenPosition > numberOfEntries) {
      return null;
    }

    Node currentNode = firstNode;
    for (int position = 1; position < givenPosition; position++) {
      currentNode = currentNode.next;
    }
    return currentNode.data;
  }

  @Override
  public boolean contains(T anEntry) {
    Node currentNode = firstNode;
    while (currentNode != null) {
      int comparison = anEntry.compareTo(currentNode.data);
      if (comparison < 0) {
        return false;
      }
      if (anEntry.equals(currentNode.data)) {
        return true;
      }
      currentNode = currentNode.next;
    }
    return false;
  }

  @Override
  public final void clear() {
    firstNode = null;
    numberOfEntries = 0;
  }

  @Override
  public int getNumberOfEntries() {
    return numberOfEntries;
  }

  @Override
  public boolean isEmpty() {
    return (numberOfEntries == 0);
  }

  @Override
  public Iterator<T> iterator() {
    return new SortedLinkedListIterator();
  }

  private class SortedLinkedListIterator implements Iterator<T> {
    private Node currentNode = firstNode;

    @Override
    public boolean hasNext() {
      return currentNode != null;
    }

    @Override
    public T next() {
      if (!hasNext()) {
        throw new NoSuchElementException("No more entries in the sorted list.");
      }
      T result = currentNode.data;
      currentNode = currentNode.next;
      return result;
    }
  }

  @Override
  public String toString() {
    StringBuilder output = new StringBuilder();
    Node currentNode = firstNode;
    while (currentNode != null) {
      output.append(currentNode.data).append('\n');
      currentNode = currentNode.next;
    }
    return output.toString();
  }

  private class Node {

    private T data;
    private Node next;

    private Node(T data) {
      this.data = data;
      next = null;
    }

    private Node(T data, Node next) {
      this.data = data;
      this.next = next;
    }
  }
}
