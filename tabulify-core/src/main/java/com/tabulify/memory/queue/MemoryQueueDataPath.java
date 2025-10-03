package com.tabulify.memory.queue;

import com.tabulify.memory.MemoryConnection;
import com.tabulify.memory.MemoryDataPath;
import com.tabulify.memory.MemoryDataPathAbs;
import com.tabulify.memory.MemoryDataPathType;
import com.tabulify.memory.list.MemoryListDataPath;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.SchemaType;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.transfer.TransferPropertiesSystem;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MemoryQueueDataPath extends MemoryDataPathAbs implements MemoryDataPath {


  /**
   * Blocking timeout properties (s)
   * See {@link #setTimeout(int)}
   */
  private int timeout = DEFAULT_TIME_OUT;

  public static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;
  public static final Integer DEFAULT_TIME_OUT = 10;
  private ArrayBlockingQueue<List<Object>> values;

  public MemoryQueueDataPath(MemoryConnection memoryConnection, String path) {
    super(memoryConnection, path, MemoryDataPathType.QUEUE);
  }

  /**
   * @param timeoutInSec - a timeout in seconds used only when the structure is {@link MemoryDataPathType#TYPE_BLOCKED_QUEUE | blocking }
   * @return a {@link MemoryListDataPath} instance for chaining initialization
   */
  public MemoryQueueDataPath setTimeout(int timeoutInSec) {
    this.timeout = timeoutInSec;
    return this;
  }

  /**
   * See {@link #setTimeout(int)}
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


  @Override
  public void truncate() {
    this.create();
  }

  @Override
  public Long getCount() {
    return (long) this.values.size();
  }

  @Override
  public void create() {
    this.values = new ArrayBlockingQueue<>(capacity);
  }

  public ArrayBlockingQueue<List<Object>> getValues() {
    return this.values;
  }

  @Override
  public InsertStream getInsertStream(DataPath source, TransferPropertiesSystem transferProperties) {
    return new MemoryQueueInsertStream(this);
  }

  @Override
  public SelectStream getSelectStream() {
    return new MemoryQueueSelectStream(this);
  }

  @Override
  public DataPath getParent() {
    return this.getConnection().getCurrentDataPath();
  }

  @Override
  public boolean hasHeaderInContent() {
    return false;
  }

  @Override
  public SchemaType getSchemaType() {
    return SchemaType.STRICT;
  }

  @Override
  public Long getSize() {
    return (long) this.values.size();
  }

}
