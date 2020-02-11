package net.bytle.db.memory;

import net.bytle.db.DbLoggers;
import net.bytle.db.database.DataStore;
import net.bytle.db.model.DataType;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.TableSystem;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.transfer.TransferListener;
import net.bytle.db.transfer.TransferProperties;
import net.bytle.log.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


/**
 * Static memory system
 * No data in their please
 */
public class MemoryDataSystem extends TableSystem {

  private static final Log LOGGER = DbLoggers.LOGGER_DB_ENGINE;


  static MemoryDataSystem memoryDataSystem;


  public static MemoryDataSystem of() {
    if (memoryDataSystem == null) {
      memoryDataSystem = new MemoryDataSystem();
    }
    return memoryDataSystem;
  }


  public void delete(DataPath memoryTable) {
    Object values = memoryTable.getDataStore().getMemoryStore().remove(memoryTable);
    if (values == null) {
      LOGGER.warning("The table (" + memoryTable + ") had no values. Nothing removed.");
    }
  }

  public void drop(DataPath memoryTable) {
    delete(memoryTable);
  }

  public void truncate(DataPath memoryTable) {
    memoryTable.getDataStore().getMemoryStore().put(memoryTable, new ArrayList<>());
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


  @Override
  public Boolean isEmpty(DataPath queue) {

    throw new RuntimeException("Not yet implemented");

  }


  @Override
  public Integer size(DataPath dataPath) {

    return ((MemoryDataPath) dataPath).getDataStore().getMemoryStore().getValues(dataPath).size();

  }

  @Override
  public boolean isDocument(DataPath dataPath) {
    throw new RuntimeException("Not implemented");
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
  public DataStore createDataStore(String name, String url) {
    return new MemoryDataStore(name, url, this);
  }


  @Override
  public Boolean exists(DataPath dataPath) {
    return ((MemoryDataPath) dataPath).getDataStore().getMemoryStore().containsKey(dataPath);
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
        memoryDataPath.getDataStore().getMemoryStore().put(dataPath, queue);
        break;
      case TYPE_LIST:
        memoryDataPath.getDataStore().getMemoryStore().put(dataPath, new ArrayList<ArrayList<Object>>());
        break;
      default:
        throw new RuntimeException("Type (" + memoryDataPath.getType() + " is unknown");
    }
  }


  @Override
  public DataType getDataType(Integer typeCode) {
    return null;
  }



}
