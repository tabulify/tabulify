package net.bytle.db.memory;


import net.bytle.db.spi.DataPath;

import java.util.Arrays;
import java.util.List;

public class MemoryDataPath extends DataPath {

    private final String[] names ;
    private final MemoryDataSystem memoryDataSystem;

    public final static int TYPE_LIST = 0;

    /**
     * See {@link #setBlocking(boolean)}
     */
    public final static int TYPE_BLOCKED_QUEUE = 1;

    /**
     * Default type
     */
    private int type = TYPE_LIST;


    /**
     * Blocking timeout properties (ms)
     * See {@link #setTimeout(long)}
     */
    private int timeout = 10000;

    /**
     * The capacity of the structure
     * See {@link #setCapacity(Integer)}
     */
    private Integer capacity = Integer.MAX_VALUE;


    public MemoryDataPath(MemoryDataSystem memoryDataSystem, String[] names) {
        this.memoryDataSystem = memoryDataSystem;
        this.names = names;
    }

    public static MemoryDataPath of(MemoryDataSystem memoryDataSystem, String[] names) {
        assert names.length!=0:"Names should not be empty";
        return new MemoryDataPath(memoryDataSystem,names);
    }

    public static MemoryDataPath of(String... names) {
        return of(MemoryDataSystem.of(),names);
    }

    @Override
    public MemoryDataSystem getDataSystem() {
        return memoryDataSystem;
    }




    @Override
    public String getName() {
        return names[names.length-1];
    }

    @Override
    public List<String> getPathParts() {
        return Arrays.asList(names);
    }

    @Override
    public String getPath() {
        return String.join(".",names);
    }

    public int getType() {
        return type;
    }

    /**
     * Ask for a blocking structure
     *
     * ie :
     *   * wait for the queue to become non-empty when retrieving an element,
     *   * wait for space to become available in the queue when storing an element.
     *
     * You can set :
     *   * the timeout with the function {@link #setTimeout(long)}
     *   * the capacity with the function {@link #setCapacity(Integer)}
     *
     * Implemented with a {@link java.util.concurrent.ArrayBlockingQueue}
     *
     * @param b - true or false
     * @return a {@link MemoryDataPath} instance for chaining initialization
     */
    public MemoryDataPath setBlocking(boolean b) {
        if (b){
            type = TYPE_BLOCKED_QUEUE;
        }
        return this;
    }

    /**
     *
     * @param timeOut - a timeout used only when the structure is {@link #setBlocking(boolean) | blocking }
     * @return a {@link MemoryDataPath} instance for chaining initialization
     */
    public MemoryDataPath setTimeout(long timeOut) {
        this.timeout = timeout;
        return this;
    }

    public long getTimeOut() {
        return this.timeout;
    }

    /**
     *
     * @param capacity - the max number of element that this path may have
     * @return a {@link MemoryDataPath} instance for chaining initialization
     *
     * This property is used when this is a {@link #setBlocking(boolean) | blocking structure}
     */
    public MemoryDataPath setCapacity(Integer capacity) {
        this.capacity = capacity;
        return this;
    }

    public Integer getCapacity() {
        return this.capacity;
    }
}
