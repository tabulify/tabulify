package net.bytle.db.memory;

import net.bytle.db.DbLoggers;
import net.bytle.db.database.DataStore;
import net.bytle.db.model.DataType;
import net.bytle.db.spi.*;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.transfer.TransferListener;
import net.bytle.db.transfer.TransferProperties;
import net.bytle.db.uri.DataUri;
import net.bytle.log.Log;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


public class MemoryDataSystem extends TableSystem {

  private static final Log LOGGER = DbLoggers.LOGGER_DB_ENGINE;

  private final MemorySystemProvider memoryStoreProvider;
  private final MemoryStore memoryStore;
  private DataStore dataStore = DataStore.of("memory");

  public MemoryDataSystem(MemorySystemProvider memorySystemProvider) {
    this.memoryStoreProvider = memorySystemProvider;
    this.memoryStore = new MemoryStore();
  }

  public static MemoryDataSystem of(MemorySystemProvider memorySystemProvider) {
    return new MemoryDataSystem(memorySystemProvider);
  }

  public static MemoryDataSystem of() {
    return MemoryDataSystem.of(MemorySystemProvider.of());
  }

  public void delete(DataPath memoryTable) {
    Object values = memoryStore.remove(memoryTable);
    if (values == null) {
      LOGGER.warning("The table (" + memoryTable + ") had no values. Nothing removed.");
    }
  }

  public void drop(DataPath memoryTable) {
    delete(memoryTable);
  }

  public void truncate(DataPath memoryTable) {
    memoryStore.put(memoryTable, new ArrayList<>());
  }


  @Override
  public TableSystemProvider getProvider() {
    return this.memoryStoreProvider;
  }


  public InsertStream getInsertStream(DataPath dataPath) {

    MemoryDataPath memoryDataPath = (MemoryDataPath) dataPath;
    return new MemoryInsertStream(memoryDataPath);

  }

  @Override
  public List<DataPath> getChildrenDataPath(DataPath dataPath) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public void move(DataPath source, DataPath target, TransferProperties transferProperties) {
    throw new RuntimeException("Not yet implemented");
  }

  /**
   * @return The number of thread that can be created against the data system
   */
  @Override
  public Integer getMaxWriterConnection() {

    throw new RuntimeException("Not yet implemented");

  }

  @Override
  public Boolean isEmpty(DataPath queue) {

    throw new RuntimeException("Not yet implemented");

  }


  @Override
  public Integer size(DataPath dataPath) {

      return ((MemoryDataPath) dataPath).getDataSystem().getMemoryStore().getValues(dataPath).size();

  }

  @Override
  public boolean isDocument(DataPath dataPath) {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public DataPath getCurrentPath() {
    return getDataPath("");
  }


  @Override
  public String getString(DataPath dataPath) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public TransferListener copy(DataPath source, DataPath target, TransferProperties transferProperties) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public TransferProperties insert(DataPath source, DataPath target, TransferProperties transferProperties) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public List<DataPath> getDescendants(DataPath dataPath) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public List<DataPath> getDescendants(DataPath dataPath, String glob) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public List<DataPath> getReferences(DataPath dataPath) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public ProcessingEngine getProcessingEngine() {
    throw new RuntimeException("Not implemented");
  }


  @Override
  public DataPath getDataPath(DataUri dataUri) {
    return MemoryDataPath.of(this, dataUri);
  }


  @Override
  public MemoryDataPath getDataPath(String... names) {
    DataUri dataUri = DataUri.of(String.join(MemoryDataPath.PATH_SEPARATOR,names) + DataUri.AT_STRING + this.getDataStore().getName());
    MemoryDataPath memoryDataPath = MemoryDataPath.of(this, dataUri);
    return memoryDataPath;
  }

  @Override
  public Boolean exists(DataPath dataPath) {
    return memoryStore.containsKey(dataPath);
  }

  @Override
  public net.bytle.db.stream.SelectStream getSelectStream(DataPath memoryTable) {

    MemoryDataPath memoryDataPath = (MemoryDataPath) memoryTable;
    switch (memoryDataPath.getType()) {
      case TYPE_BLOCKED_QUEUE:
        throw new RuntimeException("Not yet implemented");
      case TYPE_LIST:
        return new MemorySelectStream(memoryDataPath);
      default:
        throw new RuntimeException("Type (" + memoryDataPath.getType() + ") is unknown");
    }


  }

  @Override
  public DataStore getDataStore() {
    return dataStore;
  }


  @Override
  public boolean isContainer(DataPath dataPath) {

    return false;

  }

  @Override
  public void create(DataPath dataPath) {
    MemoryDataPath memoryDataPath = (MemoryDataPath) dataPath;
    switch (memoryDataPath.getType()) {
      case TYPE_BLOCKED_QUEUE:
        int bufferSize = 10000;
        BlockingQueue<List<Object>> queue = new ArrayBlockingQueue<>(bufferSize);
        memoryStore.put(dataPath, queue);
        break;
      case TYPE_LIST:
        memoryStore.put(dataPath, new ArrayList<ArrayList<Object>>());
        break;
      default:
        throw new RuntimeException("Type (" + memoryDataPath.getType() + " is unknown");
    }
  }

  @Override
  public String getProductName() {
    return "memory";
  }

  @Override
  public DataType getDataType(Integer typeCode) {
    return null;
  }

  /**
   * Closes this resource, relinquishing any underlying resources.
   * This method is invoked automatically on objects managed by the
   * {@code try}-with-resources statement.
   *
   * <p>While this interface method is declared to throw {@code
   * Exception}, implementers are <em>strongly</em> encouraged to
   * declare concrete implementations of the {@code close} method to
   * throw more specific exceptions, or to throw no exception at all
   * if the close operation cannot fail.
   *
   * <p> Cases where the close operation may fail require careful
   * attention by implementers. It is strongly advised to relinquish
   * the underlying resources and to internally <em>mark</em> the
   * resource as closed, prior to throwing the exception. The {@code
   * close} method is unlikely to be invoked more than once and so
   * this ensures that the resources are released in a timely manner.
   * Furthermore it reduces problems that could arise when the resource
   * wraps, or is wrapped, by another resource.
   *
   * <p><em>Implementers of this interface are also strongly advised
   * to not have the {@code close} method throw {@link
   * InterruptedException}.</em>
   * <p>
   * This exception interacts with a thread's interrupted status,
   * and runtime misbehavior is likely to occur if an {@code
   * InterruptedException} is {@linkplain Throwable#addSuppressed
   * suppressed}.
   * <p>
   * More generally, if it would cause problems for an
   * exception to be suppressed, the {@code AutoCloseable.close}
   * method should not throw it.
   *
   * <p>Note that unlike the {@link Closeable#close close}
   * method of {@link Closeable}, this {@code close} method
   * is <em>not</em> required to be idempotent.  In other words,
   * calling this {@code close} method more than once may have some
   * visible side effect, unlike {@code Closeable.close} which is
   * required to have no effect if called more than once.
   * <p>
   * However, implementers of this interface are strongly encouraged
   * to make their {@code close} methods idempotent.
   *
   * @throws Exception if this resource cannot be closed
   */
  @Override
  public void close() {

  }

  public MemoryStore getMemoryStore() {
    return this.memoryStore;
  }
}
