package net.bytle.vertx.collections;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * A queue cache backed by a store (database)
 */
public class WriteThroughQueue<E> implements Queue<E> {

  private final LinkedList<E> queue;
  private final WriteThroughQueueDatabaseSink<E> writeThrough;


  public WriteThroughQueue(Builder<E> eBuilder) {
    this.queue = new LinkedList<>();
    this.writeThrough = new WriteThroughQueueDatabaseSink<>(eBuilder);
  }

  @SuppressWarnings("unused")
  public static <E> Builder<E> builder(WriteThroughCollection writeThroughCollection, Class<E> clazz, String name) {
    return new Builder<>(writeThroughCollection, name);
  }

  @Override
  public int size() {
    return this.queue.size();
  }

  @Override
  public boolean isEmpty() {
    return this.queue.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return this.queue.contains(o);
  }

  @NotNull
  @Override
  public Iterator<E> iterator() {
    return this.queue.iterator();
  }

  @NotNull
  @Override
  public Object[] toArray() {
    return this.queue.toArray();
  }

  @NotNull
  @Override
  public <T> T[] toArray(@NotNull T[] a) {
    return this.queue.toArray(a);
  }

  @Override
  public boolean add(E e) {
    this.writeThrough.addToTail(e);
    return this.queue.add(e);
  }

  @Override
  public boolean remove(Object o) {
    this.writeThrough.remove(o);
    return this.queue.remove(o);
  }

  @Override
  public boolean containsAll(@NotNull Collection<?> c) {
    return this.queue.containsAll(c);
  }

  @Override
  public boolean addAll(@NotNull Collection<? extends E> c) {
    boolean added = false;
    for (E e : c) {
      boolean addedElement = this.add(e);
      if (addedElement) {
        added = true;
      }
    }
    return added;
  }

  @Override
  public boolean removeAll(@NotNull Collection<?> c) {
    boolean removed = false;
    for (Object e : c) {
      boolean elementWasRemoved = this.remove(e);
      if (elementWasRemoved) {
        removed = true;
      }
    }
    return removed;
  }

  @Override
  public boolean retainAll(@NotNull Collection<?> c) {
    return this.queue.retainAll(c);
  }

  @Override
  public void clear() {
    this.writeThrough.clear();
    this.queue.clear();
  }

  @Override
  public boolean offer(E e) {
    this.writeThrough.addToTail(e);
    return this.queue.offer(e);
  }

  @Override
  public E remove() {
    this.writeThrough.removeHead();
    return this.queue.remove();
  }

  @Override
  public E poll() {
    this.writeThrough.removeHead();
    return this.queue.poll();
  }

  @Override
  public E element() {
    return this.queue.element();
  }

  @Override
  public E peek() {
    return this.queue.peek();
  }

  /**
   * Load the elements
   */
  public void loadElementsFromSink() {
    this.writeThrough.initAll(this.queue);
  }

  public static class Builder<E> {

    final String queueName;
    final WriteThroughCollection writeThroughCollection;
    WriteThroughElementSerializer<E> serializer;

    public Builder(WriteThroughCollection writeThroughCollection, String name) {
      this.queueName = name;
      this.writeThroughCollection = writeThroughCollection;
    }

    public <E1 extends E> Builder<E1> setSerializer(WriteThroughElementSerializer<E> serializer) {
      @SuppressWarnings("unchecked") Builder<E1> self = (Builder<E1>) this;
      this.serializer = serializer;
      return self;
    }
    public WriteThroughQueue<E> build() {
      assert serializer != null;
      return new WriteThroughQueue<>(this);
    }

  }
}
