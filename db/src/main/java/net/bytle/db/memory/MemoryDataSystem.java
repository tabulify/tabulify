package net.bytle.db.memory;

import net.bytle.db.DbLoggers;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataSystem;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.transfer.TransferListener;
import net.bytle.db.transfer.TransferProperties;
import net.bytle.log.Log;

import java.util.List;


/**
 * Static memory system
 * No data in their please
 */
public class MemoryDataSystem implements DataSystem {

  private static final Log LOGGER = DbLoggers.LOGGER_DB_ENGINE;



  static MemoryDataSystem memoryDataSystem;


  public static MemoryDataSystem of() {
    if (memoryDataSystem == null) {
      memoryDataSystem = new MemoryDataSystem();
    }
    return memoryDataSystem;
  }


  /**
   * This operation does nothing for now, as the memory structure are managed by the garbage collector
   * @param memoryDataPath
   */
  public void delete(DataPath memoryDataPath) {
  }

  /**
   * This operation set the values to null as the memory structure are managed by the garbage collector
   * @param dataPath
   */
  public void drop(DataPath dataPath) {
    ((MemoryDataPath) dataPath).drop();
  }

  public void truncate(DataPath dataPath) {
    ((MemoryDataPath) dataPath).truncate();
  }


  public InsertStream getInsertStream(DataPath dataPath) {

    return ((MemoryDataPath) dataPath).getInsertStream();

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
  public Boolean isEmpty(DataPath dataPath) {

    return ((MemoryDataPath) dataPath).size()==0;

  }


  @Override
  public long size(DataPath dataPath) {

    return ((MemoryDataPath) dataPath).size();

  }

  @Override
  public boolean isDocument(DataPath dataPath) {
    throw new RuntimeException("Not yet implemented");
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
  public Boolean exists(DataPath dataPath) {
    return ((MemoryDataPath) dataPath).exists();
  }

  @Override
  public SelectStream getSelectStream(DataPath dataPath) {

    return ((MemoryDataPath) dataPath).getSelectStream();

  }


  @Override
  public boolean isContainer(DataPath dataPath) {

    return false;

  }

  /**
   * A memory may be created after it's configuration
   * A queue for instance needs a capacity
   * @param dataPath
   */
  @Override
  public void create(DataPath dataPath) {

    ((MemoryDataPath) dataPath).create();

  }






}
