package net.bytle.db.memory;


import net.bytle.db.spi.DataPath;
import net.bytle.db.uri.DataUri;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MemoryDataPath extends DataPath {

  public static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;
  public static final Integer DEFAULT_TIME_OUT = 10;
  public static final String PATH_SEPARATOR = "/";

  private final MemoryDataSystem memoryDataSystem;
  private final DataUri dataUri;

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


  private MemoryDataPath(MemoryDataSystem memoryDataSystem, DataUri dataUri) {
    this.memoryDataSystem = memoryDataSystem;
    this.dataUri = dataUri;
  }

  protected static MemoryDataPath of(MemoryDataSystem memoryDataSystem, DataUri dataUri) {
    assert dataUri.getPath() != null : "Path should not be null";
    return new MemoryDataPath(memoryDataSystem, dataUri);
  }

  /**
   * A shortcut to create a memory datapath
   *
   * @param name
   * @return
   */
  public static MemoryDataPath of(String name) {
    return MemoryDataSystem.of().getDataPath(name);
  }


  @Override
  public MemoryDataSystem getDataSystem() {
    return memoryDataSystem;
  }


  @Override
  public String getName() {
    return getPathParts().get(getPathParts().size()-1);
  }

  @Override
  public List<String> getPathParts() {

    return Arrays.asList(dataUri.getPath().split(PATH_SEPARATOR));
  }

  @Override
  public String getPath() {

    return dataUri.getPath();

  }

  @Override
  public DataUri getDataUri() {
    return dataUri;
  }

  public MemoryDataPathType getType() {
    return type;
  }

  /**
   * @param type - the type - one value of:
   *             * {@link MemoryDataPathType#TYPE_LIST} - default
   *             * {@link MemoryDataPathType#TYPE_BLOCKED_QUEUE}
   * @return a {@link MemoryDataPath} instance for chaining initialization
   */
  public MemoryDataPath setType(MemoryDataPathType type) {
    this.type = type;
    return this;
  }

  /**
   * @param timeOut - a timeout in seconds used only when the structure is {@link MemoryDataPathType#TYPE_BLOCKED_QUEUE | blocking }
   * @return a {@link MemoryDataPath} instance for chaining initialization
   */
  public MemoryDataPath setTimeout(long timeOut) {
    this.timeout = timeout;
    return this;
  }

  /**
   * See {@link #setTimeout(long)}
   *
   * @return Timeout en seconds
   */
  public Integer getTimeOut() {
    return this.timeout;
  }

  /**
   * @param capacity - the max number of element that this path may have
   * @return a {@link MemoryDataPath} instance for chaining initialization
   * <p>
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
