package net.bytle.db.memory;


import net.bytle.db.spi.DataPath;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MemoryDataPath extends DataPath {

    public static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;
    public static final Integer DEFAULT_TIME_OUT = 10;

    private final String[] names ;
    private final MemoryDataSystem memoryDataSystem;




    /**
     * Default type
     */
    private MemoryDataPathType type = MemoryDataPathType.TYPE_LIST;


    /**
     * Blocking timeout properties (s)
     * See {@link #setTimeout(long)}
     */
    private int timeout = DEFAULT_TIME_OUT;

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

    public MemoryDataPathType getType() {
        return type;
    }

    /**
     *
     * @param type - the type - one value of:
     *             * {@link MemoryDataPathType#TYPE_LIST} - default
     *             * {@link MemoryDataPathType#TYPE_BLOCKED_QUEUE}
     *
     * @return a {@link MemoryDataPath} instance for chaining initialization
     */
    public MemoryDataPath setType(MemoryDataPathType type) {
        this.type = type;
        return this;
    }

    /**
     *
     * @param timeOut - a timeout in seconds used only when the structure is {@link MemoryDataPathType#TYPE_BLOCKED_QUEUE | blocking }
     * @return a {@link MemoryDataPath} instance for chaining initialization
     *
     */
    public MemoryDataPath setTimeout(long timeOut) {
        this.timeout = timeout;
        return this;
    }

    /**
     * See {@link #setTimeout(long)}
     * @return Timeout en seconds
     */
    public Integer getTimeOut() {
        return this.timeout;
    }

    /**
     *
     * @param capacity - the max number of element that this path may have
     * @return a {@link MemoryDataPath} instance for chaining initialization
     *
     * This property is used when this is a {@link #setType(MemoryDataPathType)}  | blocking structure}
     */
    public MemoryDataPath setCapacity(Integer capacity) {
        this.capacity = capacity;
        return this;
    }

    public Integer getCapacity() {
        return this.capacity;
    }
}
