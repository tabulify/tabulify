package com.tabulify.type;

import java.util.*;

/**
 * A tail queue is a fixed size queue that:
 *   * add the last element to the tail
 *   * and remove the head if the queue is over capacity.
 * @param <E>
 */
public final class TailQueue<E> extends AbstractQueue<E> {

  private final ArrayDeque<E> queue;
  final int maxSize;


  /**
   * A tail queue is a fixed size queue
   * that add the last element to the tail and remove the head if the queue is over capacity.
   * @param maxSize - the max number of element. It should be greater than zero
   */
  public TailQueue(Integer maxSize) {
    assert maxSize>0: "The size should be a positive integer";
    this.queue = new ArrayDeque<>(maxSize);
    this.maxSize = maxSize;
  }



  /**
   * Returns the number of additional elements that this queue can accept without evicting; zero if
   * the queue is currently full.
   *
   */
  public int remainingCapacity() {
    return maxSize - size();
  }


  /**
   *
   * See {@link #add(Object)} operations.
   *
   * @return {@code true} always
   */
  @Override
  public boolean offer(E e) {
    return add(e);
  }

  @Override
  public E poll() {
    return queue.poll();
  }

  @Override
  public E peek() {
    return queue.peek();
  }

  /**
   * Adds the given element to the tail of the queue.
   * If the queue is currently full, the element at the head of the queue is evicted to make room.
   *
   * @return {@code true} always
   */
  @Override
  public boolean add(E e) {
    assert e!=null;
    if (size() == maxSize) {
      queue.removeFirst();
    }
    queue.addLast(e);
    return true;
  }

  @Override
  public boolean addAll(Collection<? extends E> collection) {
    collection.forEach(this::add);
    return true;
  }

  @Override
  public Iterator<E> iterator() {
    return queue.iterator();
  }

  @Override
  public int size() {
    return queue.size();
  }

  @Override
  public boolean contains(Object object) {
    assert object!=null;
    return queue.contains(object);
  }

  @Override
  public boolean remove(Object object) {
    assert object!=null;
    return queue.remove(object);
  }

  public E getLast(){
    return queue.getLast();
  }


}

