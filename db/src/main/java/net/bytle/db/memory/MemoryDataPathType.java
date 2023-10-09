package net.bytle.db.memory;

import net.bytle.type.MediaType;

public enum MemoryDataPathType implements MediaType {

  /**
   * a list structure
   */
  TYPE_LIST(false),
  /**
   * a blocking structure
   * <p>
   * ie :
   * * wait for the queue to become non-empty when retrieving an element,
   * * wait for space to become available in the queue when storing an element.
   * <p>
   * You can set :
   * * the timeout with the function {@link net.bytle.db.memory.queue.MemoryQueueDataPath#setTimeout(int)}
   * * the capacity with the function {@link net.bytle.db.memory.queue.MemoryQueueDataPath#setCapacity(Integer)}}
   * <p>
   * Implemented with a {@link java.util.concurrent.ArrayBlockingQueue}
   */
  TYPE_BLOCKED_QUEUE(false),
  LIST(false),
  QUEUE(false),
  NAMESPACE(true);

  private final boolean isContainer;

  MemoryDataPathType(boolean isContainer) {
    this.isContainer = isContainer;
  }

  @Override
  public String toString() {
    return this.getType() + "/" + this.getSubType();
  }


  @Override
  public String getSubType() {
    return super.name().toLowerCase();
  }

  @Override
  public String getType() {
    return "mem";
  }

  @Override
  public boolean isContainer() {
    return this.isContainer;
  }

  @Override
  public String getExtension() {
    return this.getSubType();
  }

}
