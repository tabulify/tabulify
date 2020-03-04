package net.bytle.db.memory;

import net.bytle.db.model.RelationDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataSystem;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.transfer.TransferListener;
import net.bytle.db.transfer.TransferProperties;
import net.bytle.type.Strings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * No data in their please
 */
public class MemoryDataSystem implements DataSystem {

  /**
   * The data path of the data store are keep here
   * This is to be able to support the copy/merge of data defs
   * into another data path that have foreign key relationships
   * <p>
   * ie if the foreign table exists, we create the foreign key
   * * in a {@link net.bytle.db.model.DataDefs#merge(RelationDef, RelationDef)}
   * * or {@link net.bytle.db.model.DataDefAbs#copyDataDef(DataPath)}
   */
  Map<String, MemoryDataPath> storageMemDataPaths = new HashMap<>();

  private MemoryDataStore dataStore;

  public MemoryDataSystem(MemoryDataStore memoryDataStore) {
    this.dataStore = memoryDataStore;
  }


  /**
   * @param memoryDataPath
   */
  public void delete(DataPath memoryDataPath) {
  }


  public void truncate(DataPath dataPath) {
    ((MemoryDataPath) dataPath).truncate();
  }


  public InsertStream getInsertStream(DataPath dataPath) {

    return ((MemoryDataPath) dataPath).getInsertStream();

  }

  @Override
  public List<DataPath> getChildrenDataPath(DataPath dataPath) {
    if (dataPath.equals(dataPath.getDataStore().getCurrentDataPath())){
      return  storageMemDataPaths.values()
        .stream()
        .filter(p->!p.equals(dataPath.getDataStore().getCurrentDataPath()))
        .collect(Collectors.toList());
    } else {
      throw new RuntimeException(Strings.multiline(
        "The data path ("+dataPath+") is not the root path.",
        "In the actual implementation, only the children from the root path may be retrieved"
      ));
    }
  }

  @Override
  public void move(DataPath source, DataPath target, TransferProperties transferProperties) {
    throw new RuntimeException("Not yet implemented");
  }


  @Override
  public Boolean isEmpty(DataPath dataPath) {

    return ((MemoryDataPath) dataPath).size() == 0;

  }


  @Override
  public long size(DataPath dataPath) {

    return ((MemoryDataPath) dataPath).size();

  }

  @Override
  public boolean isDocument(DataPath dataPath) {
    if (dataPath.equals(dataPath.getDataStore().getCurrentDataPath())){
      return false;
    } else {
      return true;
    }
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
    throw new RuntimeException(Strings.multiline(
      "Not yet implemented.",
      "If you want to select a file from a tabular object, you need to set the default data store to the file system")
    );
  }

  @Override
  public List<DataPath> getReferences(DataPath dataPath) {
    throw new RuntimeException("Not yet implemented");
  }


  @Override
  public MemoryDataStore getDataStore() {
    return dataStore;
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
   *
   * @param dataPath
   */
  @Override
  public void create(DataPath dataPath) {

    // Create the structure
    ((MemoryDataPath) dataPath).create();

  }

  @Override
  public void drop(DataPath dataPath) {
    MemoryDataPath returned = storageMemDataPaths.remove(dataPath.getPath());
    if (returned == null) {
      throw new RuntimeException("The data path (" + dataPath + ") could not be dropped because it does not exists");
    }
  }

  @Override
  public Boolean exists(DataPath dataPath) {
    return storageMemDataPaths.containsKey(dataPath.getPath());
  }


}
