package adt;

import java.util.Iterator;

public interface PriorityQueueInterface<T> {

  public void enqueue(T newEntry); // add new item and place it based on priority

  public T dequeue(); // remove and return the highest priority item

  public T getFront(); // view highest priority item without removing it

  public boolean isEmpty(); // check whether the queue has no items

  public void clear(); // remove all items from the queue

  public int getNumberOfEntries(); // return total number of items

  public Iterator<T> getIterator(); // allow items to be viewed one by one
}
