package net.bytle.db.spi;

import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.transfer.TransferListener;
import net.bytle.db.transfer.TransferProperties;

import java.util.List;

public interface DataSystem {


  Boolean exists(DataPath dataPath);

  SelectStream getSelectStream(DataPath dataPath);

  boolean isContainer(DataPath dataPath);

  void create(DataPath dataPath);


  void drop(DataPath dataPath);

  void delete(DataPath dataPath);

  void truncate(DataPath dataPath);


  InsertStream getInsertStream(DataPath dataPath);

  List<DataPath> getChildrenDataPath(DataPath dataPath);

  void move(DataPath source, DataPath target, TransferProperties transferProperties);



  Boolean isEmpty(DataPath queue);

  long size(DataPath dataPath);

  /**
   * @param dataPath
   * @return true if the data path locate a document
   * <p>
   * The opposite is {@link #isContainer(DataPath)}
   */
  boolean isDocument(DataPath dataPath);


  /**
   * @param dataPath
   * @return the content of a data path in a string format
   */
  String getString(DataPath dataPath);

  TransferListener copy(DataPath source, DataPath target, TransferProperties transferProperties);

  TransferProperties insert(DataPath source, DataPath target, TransferProperties transferProperties);

  /**
   * @param dataPath the ancestor data path
   * @return the descendants of the data path
   */
  List<DataPath> getDescendants(DataPath dataPath);

  /**
   * @param dataPath a data path container (a directory, a schema or a catalog)
   * @param glob     a glob that filters the descendant data path returned
   * @return the descendant data paths representing sql tables, schema or files
   */
  List<DataPath> getDescendants(DataPath dataPath, String glob);

  /**
   * @param dataPath the data path
   * @return data paths that references the data path primary key (via foreign keys)
   */
  List<DataPath> getReferences(DataPath dataPath);


}
