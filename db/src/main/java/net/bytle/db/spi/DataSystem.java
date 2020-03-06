package net.bytle.db.spi;

import net.bytle.db.database.DataStore;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.transfer.TransferListener;

import java.util.List;

/**
 * A data system
 *   * a file system
 *   * a relational database
 *   * a memory system
 *
 * The data system is where the data is stored
 *   * a file system store them on the file system
 *   * the relational database store them remotely on the server
 *   * memory system store them in a map
 */
public interface DataSystem {


  DataStore getDataStore();

  Boolean exists(DataPath dataPath);

  SelectStream getSelectStream(DataPath dataPath);

  boolean isContainer(DataPath dataPath);

  void create(DataPath dataPath);

  void drop(DataPath dataPath);

  void delete(DataPath dataPath);

  void truncate(DataPath dataPath);


  InsertStream getInsertStream(DataPath dataPath);

  List<DataPath> getChildrenDataPath(DataPath dataPath);

  void move(DataPath source, DataPath target);


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

  /**
   * Cop the source to the target
   * @param source
   * @param target
   * @return
   */
  TransferListener copy(DataPath source, DataPath target);

  /**
   * Insert the source data into the target
   * @param source
   * @param target
   * @return
   */
  TransferListener insert(DataPath source, DataPath target);

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
