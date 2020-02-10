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
  private String path;


  private MemoryDataPath(MemoryDataSystem memoryDataSystem, String path) {
    this.memoryDataSystem = memoryDataSystem;
    this.path = path;
  }

  protected static MemoryDataPath of(MemoryDataSystem memoryDataSystem, String path) {
    return new MemoryDataPath(memoryDataSystem, path);
  }


  @Override
  public MemoryDataSystem getDataSystem() {
    return memoryDataSystem;
  }


  @Override
  public String getName() {
    return getNames().get(getNames().size()-1);
  }

  @Override
  public List<String> getNames() {

    return Arrays.asList(this.path.split(PATH_SEPARATOR));
  }

  @Override
  public String getPath() {

    return path;

  }

  @Override
  public DataUri getDataUri() {
    return DataUri.of().setDataStore(this.memoryDataSystem.getDataStore().getName()).setPath(path);
  }

  @Override
  public MemoryDataPath getSibling(String name) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public MemoryDataPath getChild(String name) {

    if (this.path ==null) {
      return new MemoryDataPath(memoryDataSystem, name);
    } else {
      return new MemoryDataPath(memoryDataSystem, this.path + PATH_SEPARATOR + name);
    }

  }

  @Override
  public MemoryDataPath resolve(String... names) {
    throw new RuntimeException("Not yet implemented");
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

  @Override
  public DataPath getSelectStreamDependency() {
    return null; // Nothing to see here
  }
}
