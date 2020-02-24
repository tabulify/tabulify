package net.bytle.db.memory.queue;

import net.bytle.db.memory.MemoryDataPath;
import net.bytle.db.memory.MemoryDataPathAbs;
import net.bytle.db.memory.MemoryDataPathType;
import net.bytle.db.memory.MemoryDataStore;
import net.bytle.db.memory.list.MemoryListDataPath;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MemoryQueueDataPath extends MemoryDataPathAbs implements MemoryDataPath {

  static final String TYPE = "QUEUE";

  /**
   * Blocking timeout properties (s)
   * See {@link #setTimeout(long)}
   */
  private int timeout = DEFAULT_TIME_OUT;

  public static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;
  public static final Integer DEFAULT_TIME_OUT = 10;
  private ArrayBlockingQueue<List<Object>> values;

  public MemoryQueueDataPath(MemoryDataStore memoryDataStore, String path) {
    super(memoryDataStore, path);
  }

  /**
   * @param timeOut - a timeout in seconds used only when the structure is {@link MemoryDataPathType#TYPE_BLOCKED_QUEUE | blocking }
   * @return a {@link MemoryListDataPath} instance for chaining initialization
   */
  public MemoryQueueDataPath setTimeout(long timeOut) {
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
   * @return a {@link MemoryListDataPath} instance for chaining initialization
   * <p>
   *
   */
  public MemoryQueueDataPath setCapacity(Integer capacity) {
    this.capacity = capacity;
    return this;
  }

  public Integer getCapacity() {
    return this.capacity;
  }

  /**
   * The capacity of the structure
   * See {@link #setCapacity(Integer)}
   */
  private Integer capacity = Integer.MAX_VALUE;

  public String getType() {
    return TYPE;
  }

  @Override
  public void truncate() {
    this.create();
  }

  @Override
  public long size() {
    return this.values.size();
  }

  @Override
  public void create() {
    this.values = new ArrayBlockingQueue<>(capacity);
  }

  @Override
  public ArrayBlockingQueue<List<Object>> getValues() {
    return this.values;
  }

  @Override
  public InsertStream getInsertStream() {
    return new MemoryQueueInsertStream(this);
  }

  @Override
  public SelectStream getSelectStream() {
    return new MemoryQueueSelectStream(this);
  }

  @Override
  public void drop() {
    this.values = null;
  }
}
