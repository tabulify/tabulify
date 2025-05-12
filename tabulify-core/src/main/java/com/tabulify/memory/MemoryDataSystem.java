package com.tabulify.memory;

import com.tabulify.model.Constraint;
import com.tabulify.model.ForeignKeyDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataSystemAbs;
import com.tabulify.spi.SelectException;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.transfer.TransferListener;
import com.tabulify.transfer.TransferProperties;
import com.tabulify.transfer.TransferSourceTarget;
import net.bytle.exception.InternalException;
import net.bytle.regexp.Glob;
import net.bytle.type.MediaType;
import net.bytle.type.Strings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * No data in their please
 */
public class MemoryDataSystem extends DataSystemAbs {

  /**
   * The data path of the data store are keep here
   * This is to be able to support the copy/merge of data defs
   * into another data path that have foreign key relationships
   * <p>
   */
  private Map<String, MemoryDataPath> storageMemDataPaths = new HashMap<>();

  private final MemoryConnection memoryConnection;

  public MemoryDataSystem(MemoryConnection memoryConnection) {
    super(memoryConnection);
    this.memoryConnection = memoryConnection;
  }


  /**
   */
  public void delete(DataPath memoryDataPath) {
    storageMemDataPaths.remove(memoryDataPath.getRelativePath());
  }


  public void truncate(List<DataPath> dataPaths) {
    dataPaths.forEach(dataPath -> ((MemoryDataPath) dataPath).truncate());
  }


  public InsertStream getInsertStream(TransferSourceTarget transferSourceTarget) {

    return transferSourceTarget.getTargetDataPath().getInsertStream();

  }

  @SuppressWarnings("unchecked")
  @Override
  public List<MemoryDataPath> getChildrenDataPath(DataPath dataPath) {
    if (dataPath.equals(dataPath.getConnection().getCurrentDataPath())) {
      return storageMemDataPaths.values()
        .stream()
        .filter(p -> !p.equals(dataPath.getConnection().getCurrentDataPath()))
        .collect(Collectors.toList());
    } else {
      throw new RuntimeException(Strings.createMultiLineFromStrings(
        "The data path (" + dataPath + ") is not the root path.",
        "In the actual implementation, only the children from the root path may be retrieved"
      ).toString());
    }
  }


  @Override
  public Boolean isEmpty(DataPath dataPath) {

    return dataPath.getCount() == 0;

  }


  @Override
  public boolean isDocument(DataPath dataPath) {
    return !dataPath.equals(dataPath.getConnection().getCurrentDataPath());
  }


  @Override
  public String getString(DataPath dataPath) {
    int columnSize = dataPath.getOrCreateRelationDef().getColumnsSize();
    StringBuilder string = new StringBuilder();
    try (SelectStream selectString = dataPath.getSelectStream()) {
      while (selectString.next()) {
        if (string.length() > 0) {
          string.append(Strings.EOL);
        }
        for (int i = 0; i < columnSize; i++) {
          string.append(selectString.getObject(i + 1));
        }
      }
    } catch (SelectException e) {
      throw new InternalException("Select stream memory error: ", e);
    }
    return string.toString();
  }

  @Override
  public TransferListener transfer(DataPath source, DataPath target, TransferProperties transferProperties) {
    throw new UnsupportedOperationException("Not yet implemented");
  }


  @SuppressWarnings("unchecked")
  @Override
  public List<MemoryDataPath> getDescendants(DataPath dataPath) {

    /**
     * Only one level for now
     */
    return getChildrenDataPath(dataPath);

  }

  @SuppressWarnings("unchecked")
  @Override
  public List<MemoryDataPath> select(DataPath dataPath, String patternOrPath, MediaType mediaType) {

    return getChildrenDataPath(dataPath)
      .stream()
      .filter(dp -> Glob.createOf(patternOrPath).matches(dp.getName()))
      .collect(Collectors.toList());
  }

  @Override
  public List<ForeignKeyDef> getForeignKeysThatReference(DataPath dataPath) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public void dropConstraint(Constraint constraint) {
    throw new RuntimeException("The memory system does not have the notion of constraint yet");
  }


  @Override
  public MemoryConnection getConnection() {
    return memoryConnection;
  }


  @Override
  public boolean isContainer(DataPath dataPath) {

    return false;

  }

  /**
   * A memory may be created after it's configuration
   * A queue for instance needs a capacity
   *
   */
  @Override
  public void create(DataPath dataPath) {

    // Create the structure
    ((MemoryDataPath) dataPath).create();
    // Add it
    addInStore((MemoryDataPath) dataPath);

  }

  @Override
  public void drop(DataPath dataPath) {
    MemoryDataPath returned = storageMemDataPaths.remove(dataPath.getRelativePath());
    if (returned == null) {
      throw new RuntimeException("The data path (" + dataPath + ") could not be dropped because it does not exists");
    }
  }

  @Override
  public Boolean exists(DataPath dataPath) {
    return storageMemDataPaths.containsKey(dataPath.getRelativePath());
  }


  public void addInStore(MemoryDataPath memoryDataPath) {
    // Add it
    storageMemDataPaths.put(memoryDataPath.getRelativePath(), memoryDataPath);
  }

  public MemoryDataPath getFromStore(String path) {
    return storageMemDataPaths.get(path);
  }

  public void emptyStore() {
    storageMemDataPaths = new HashMap<>();
  }
}
