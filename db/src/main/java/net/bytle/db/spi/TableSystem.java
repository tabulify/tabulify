package net.bytle.db.spi;

import net.bytle.db.database.DataStore;
import net.bytle.db.model.SqlDataType;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.transfer.TransferListener;
import net.bytle.db.transfer.TransferProperties;

import java.util.List;

public abstract class TableSystem {


  public abstract Boolean exists(DataPath dataPath);

  public abstract SelectStream getSelectStream(DataPath dataPath);

  public abstract boolean isContainer(DataPath dataPath);

  public abstract void create(DataPath dataPath);


  public abstract SqlDataType getDataType(Integer typeCode);

  public abstract void drop(DataPath dataPath);

  public abstract void delete(DataPath dataPath);

  public abstract void truncate(DataPath dataPath);


  public abstract InsertStream getInsertStream(DataPath dataPath);

  public abstract List<DataPath> getChildrenDataPath(DataPath dataPath);

  public abstract void move(DataPath source, DataPath target, TransferProperties transferProperties);



  public abstract Boolean isEmpty(DataPath queue);

  public abstract Integer size(DataPath dataPath);

  /**
   * @param dataPath
   * @return true if the data path locate a document
   * <p>
   * The opposite is {@link #isContainer(DataPath)}
   */
  public abstract boolean isDocument(DataPath dataPath);


  /**
   * @param dataPath
   * @return the content of a data path in a string format
   */
  public abstract String getString(DataPath dataPath);

  public abstract TransferListener copy(DataPath source, DataPath target, TransferProperties transferProperties);

  public abstract TransferProperties insert(DataPath source, DataPath target, TransferProperties transferProperties);

  /**
   * @param dataPath the ancestor data path
   * @return the descendants of the data path
   */
  public abstract List<DataPath> getDescendants(DataPath dataPath);

  /**
   * @param dataPath a data path container (a directory, a schema or a catalog)
   * @param glob     a glob that filters the descendant data path returned
   * @return the descendant data paths representing sql tables, schema or files
   */
  public abstract List<DataPath> getDescendants(DataPath dataPath, String glob);

  /**
   * @param dataPath the data path
   * @return data paths that references the data path primary key (via foreign keys)
   */
  public abstract List<DataPath> getReferences(DataPath dataPath);

  public abstract DataStore createDataStore(String name, String url);

}
