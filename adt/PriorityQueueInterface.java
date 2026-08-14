package adt;

import java.util.Iterator;

public interface PriorityQueueInterface<T> {

  public void enqueue(T newEntry);

  public T dequeue();

  public T getFront();

  public boolean isEmpty();

  public void clear();

  public int getNumberOfEntries();

  public Iterator<T> getIterator();
}
