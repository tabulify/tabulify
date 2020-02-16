package net.bytle.db.memory;

import net.bytle.db.memory.list.MemoryListDataPath;

public enum MemoryDataPathType {

    /**
     * a list structure chosen with {@link MemoryListDataPath#setType(MemoryDataPathType)}
     */
    TYPE_LIST,
    /**
     * a blocking structure chosen with {@link MemoryListDataPath#setType(MemoryDataPathType)}
     *
     * ie :
     *   * wait for the queue to become non-empty when retrieving an element,
     *   * wait for space to become available in the queue when storing an element.
     *
     * You can set :
     *   * the timeout with the function {@link MemoryListDataPath#setTimeout(long)}
     *   * the capacity with the function {@link MemoryListDataPath#setCapacity(Integer)}
     *
     * Implemented with a {@link java.util.concurrent.ArrayBlockingQueue}
     */
    TYPE_BLOCKED_QUEUE;


}
