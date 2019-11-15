package net.bytle.db.memory;

public enum MemoryDataPathType {

    /**
     * a list structure chosen with {@link MemoryDataPath#setType(MemoryDataPathType)}
     */
    TYPE_LIST,
    /**
     * a blocking structure chosen with {@link MemoryDataPath#setType(MemoryDataPathType)}
     *
     * ie :
     *   * wait for the queue to become non-empty when retrieving an element,
     *   * wait for space to become available in the queue when storing an element.
     *
     * You can set :
     *   * the timeout with the function {@link MemoryDataPath#setTimeout(long)}
     *   * the capacity with the function {@link MemoryDataPath#setCapacity(Integer)}
     *
     * Implemented with a {@link java.util.concurrent.ArrayBlockingQueue}
     */
    TYPE_BLOCKED_QUEUE;


}
