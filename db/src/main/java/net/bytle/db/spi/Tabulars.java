package net.bytle.db.spi;

import net.bytle.db.DbLoggers;
import net.bytle.db.engine.Dag;
import net.bytle.db.engine.DagDataPath;
import net.bytle.db.model.DataDefs;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.stream.Streams;
import net.bytle.db.transfer.TransferListener;
import net.bytle.db.transfer.TransferLoadOperation;
import net.bytle.db.transfer.TransferManager;
import net.bytle.db.transfer.TransferProperties;
import net.bytle.regexp.Globs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class Tabulars {


  public synchronized static Boolean exists(DataPath dataPath) {

    return dataPath.getDataSystem().exists(dataPath);

  }

  public static SelectStream getSelectStream(DataPath dataPath) {
    if (Tabulars.exists(dataPath)) {
      return dataPath.getDataSystem().getSelectStream(dataPath);
    } else {
      throw new RuntimeException("The data unit (" + dataPath.toString() + ") does not exist. You can't therefore ask for a select stream.");
    }
  }


  public static void create(DataPath dataPath) {

    dataPath.getDataSystem().create(dataPath);

  }

  /**
   * @param dataPaths - a list of data path
   * @return return a independent set of data path (ie independent of foreign key outside of the set)
   * <p>
   * (ie delete the foreign keys of a table if the foreign table is not part of the set)
   */
  public static List<DataPath> atomic(List<DataPath> dataPaths) {
    for (DataPath dataPath : dataPaths) {
      List<ForeignKeyDef> foreignKeys = dataPath.getDataDef().getForeignKeys();
      for (ForeignKeyDef foreignKeyDef : foreignKeys) {
        if (!(dataPaths.contains(foreignKeyDef.getForeignPrimaryKey().getDataDef().getDataPath()))) {
          dataPath.getDataDef().deleteForeignKey(foreignKeyDef);
        }
      }
    }
    return dataPaths;
  }

  /**
   * Create all data object if they don't exist
   * taking into account the foreign key constraints
   * <p>
   *
   * @param dataPaths
   */
  public static void createIfNotExist(List<DataPath> dataPaths) {

    Dag<DataPath> dag = DagDataPath.get(dataPaths);
    dataPaths = dag.getCreateOrderedTables();
    for (DataPath dataPath : dataPaths) {
      createIfNotExist(dataPath);
    }

  }

  public static boolean isContainer(DataPath dataPath) {
    return dataPath.getDataSystem().isContainer(dataPath);
  }

  /**
   * Create the table in the database if it doesn't exist
   *
   * @param dataPath
   */
  public static void createIfNotExist(DataPath dataPath) {

    if (!exists(dataPath)) {

      create(dataPath);

    } else {

      DbLoggers.LOGGER_DB_ENGINE.fine("The data object (" + dataPath.toString() + ") already exist.");

    }

  }

  public static void drop(DataPath datapath, DataPath... dataPaths) {

    List<DataPath> allDataPaths = new ArrayList<>();
    allDataPaths.add(datapath);
    allDataPaths.addAll(Arrays.asList(dataPaths));

    // A dag will build the data def and we may not want want it when dropping only one table
    Dag<DataPath> dag = DagDataPath.get(allDataPaths);
    for (DataPath dataPath : dag.getDropOrderedTables()) {
      dataPath.getDataSystem().drop(dataPath);
    }


  }

  /**
   * Drop one or more tables
   * <p>
   * If the table is a foreign table, the child constraint will
   * prevent the table to be dropped if the child table is not given.
   * <p>
   *
   * @param dataPaths - The tables to drop
   */
  public static void drop(List<DataPath> dataPaths) {
    if (dataPaths.size() == 0) {
      throw new RuntimeException("The list of data paths to drop cannot be null");
    } else {
      DataPath[] moreDataPath = {};
      if (dataPaths.size() > 1) {
        moreDataPath = dataPaths.subList(1, dataPaths.size()).toArray(new DataPath[0]);
      }
      drop(dataPaths.get(0), moreDataPath);
    }

  }

  /**
   * Suppress all rows of the table
   *
   * @param dataPath - the tableDef where to suppress all rows
   */
  public static void delete(DataPath dataPath) {

    dataPath.getDataSystem().delete(dataPath);

  }

  public static void dropIfExists(DataPath... dataPaths) {

    dropIfExists(Arrays.asList(dataPaths));

  }


  /**
   * Drop the table from the database if exist
   * and drop the table from the cache
   *
   * @param dataPaths
   */
  public static void dropIfExists(List<DataPath> dataPaths) {

    // A hack to avoid building the data def
    // Because there is for now no drop options, the getDropOrderedTables
    // will build the dependency
    if (dataPaths.size() == 1) {
      if (exists(dataPaths.get(0))) {
        drop(dataPaths.get(0));
      }
    } else {
      for (DataPath dataPath : DagDataPath.get(dataPaths).getDropOrderedTables()) {
        if (exists(dataPath)) {
          drop(dataPath);
        }
      }
    }

  }

  public static void truncate(DataPath dataPath) {
    dataPath.getDataSystem().truncate(dataPath);
  }


  /**
   * Print the data of a table
   *
   * @param dataPath
   */
  public static void print(DataPath dataPath) {

    SelectStream tableOutputStream = getSelectStream(dataPath);
    Streams.print(tableOutputStream);
    tableOutputStream.close();

  }

  public static InsertStream getInsertStream(DataPath dataPath) {
    return dataPath.getDataSystem().getInsertStream(dataPath);
  }

  public static List<DataPath> move(List<DataPath> sources, DataPath target) {

    List<DataPath> targetDataPaths = new ArrayList<>();
    for (DataPath sourceDataPath : DagDataPath.get(sources).getCreateOrderedTables()) {
      DataPath targetDataPath = target.getDataSystem().getDataPath(sourceDataPath.getName());
      Tabulars.move(sourceDataPath, targetDataPath);
      targetDataPaths.add(targetDataPath);
    }

    return targetDataPaths;

  }


  /**
   *
   * @param source - the source to move
   * @param target - a target data path container or document
   * If the target is a container, the target will have the name of the source
   */
  public static void move(DataPath source, DataPath target) {

    if (Tabulars.isContainer(target)){
      target = target.getChild(source.getName());
    }
    move(source, target, TransferProperties.of());

  }

  public static TransferListener transfer(DataPath source, DataPath target) {

    return transfer(source, target, TransferProperties.of());
  }

  public static TransferListener transfer(DataPath source, DataPath target, TransferProperties transferProperties) {

    return TransferManager.transfer(source, target, transferProperties);
  }


  public static boolean isEmpty(DataPath queue) {
    return queue.getDataSystem().isEmpty(queue);
  }


  public static int getSize(DataPath dataPath) {
    return dataPath.getDataSystem().size(dataPath);
  }

  /**
   * @param dataPath
   * @return if the data path locate a document
   * <p>
   * The counter part is {@link #isContainer(DataPath)}
   */
  public static boolean isDocument(DataPath dataPath) {
    return dataPath.getDataSystem().isDocument(dataPath);
  }

  public static void create(List<DataPath> dataPaths) {
    Dag<DataPath> dag = DagDataPath.get(dataPaths);
    dataPaths = dag.getCreateOrderedTables();
    for (DataPath dataPath : dataPaths) {
      create(dataPath);
    }
  }

  /**
   * @param dataPath - a data path container (a directory, a schema or a catalog)
   * @return the children data paths representing sql tables, schema or files
   */
  public static List<DataPath> getChildren(DataPath dataPath) {

    if (Tabulars.isDocument(dataPath)) {
      throw new RuntimeException("The data path (" + dataPath + ") is a document, it has therefore no children");
    }
    return dataPath.getDataSystem().getChildrenDataPath(dataPath);

  }

  /**
   * @param dataPath - a parent/container dataPath
   * @param glob     -  a glob pattern
   * @return the children data path of the parent that matches the glob pattern
   */
  public static List<DataPath> getChildren(DataPath dataPath, String glob) {
    final String regex = Globs.toRegexPattern(glob);
    return getChildren(dataPath)
      .stream()
      .filter(s -> s.getName().matches(regex))
      .collect(Collectors.toList());
  }



  /**
   * @param dataPath - a data path container (a directory, a schema or a catalog)
   * @return the descendant data paths representing sql tables, schema or files
   */
  public static List<DataPath> getDescendants(DataPath dataPath) {

    if (Tabulars.isDocument(dataPath)) {
      throw new RuntimeException("The data path (" + dataPath + ") is a document, it has therefore no children");
    }
    return dataPath.getDataSystem().getDescendants(dataPath);

  }



  /**
   * @param dataPath a data path container (a directory, a schema or a catalog)
   * @param glob     a glob that filters the descendant data path returned
   * @return the descendant data paths representing sql tables, schema or files
   */
  public static List<DataPath> getDescendants(DataPath dataPath, String glob) {

    if (Tabulars.isDocument(dataPath)) {
      throw new RuntimeException("The data path (" + dataPath + ") is a document, it has therefore no children");
    }
    return dataPath.getDataSystem().getDescendants(dataPath, glob);

  }

  /**
   * @param one  - the primary key table
   * @param many - the foreign key table
   * @return the dropped foreign keys
   */
  public static List<ForeignKeyDef> dropOneToManyRelationship(DataPath one, DataPath many) {

    List<ForeignKeyDef> foreignKeyDefs = one.getDataDef().getForeignKeys().stream()
      .filter(fk -> fk.getForeignPrimaryKey().getDataDef().getDataPath().equals(many))
      .collect(Collectors.toList());

    foreignKeyDefs.stream()
      .forEach(fk -> {
        throw new RuntimeException("Not yet implemented");
      });

    return foreignKeyDefs;

  }

  /**
   * @param dataPath
   * @return the content of a data path in a string format
   */
  public static String getString(DataPath dataPath) {
    return dataPath.getDataSystem().getString(dataPath);
  }

  /**
   * Move a source document to a target document
   * If the document is:
   * * on the same data store, it's a rename operation,
   * * not on the same data store, it's a transfer and a delete from the source
   *
   * @param source
   * @param target
   * @param transferProperties
   * @return a {@link TransferListener} or null if it was no transfer
   */
  public static TransferListener move(DataPath source, DataPath target, TransferProperties transferProperties) {

    TransferListener transferListener = null;

    final TableSystem sourceDataSystem = source.getDataSystem();
    if (sourceDataSystem.equals(target.getDataSystem())) {
      // same provider (fs or jdbc)
      sourceDataSystem.move(source, target, transferProperties);
    } else {
      // different provider (fs to jdbc or jdbc to fs)
      transferListener = TransferManager.transfer(source, target, transferProperties);
      Tabulars.drop(source);
    }

    return transferListener;
  }

  /**
   *
   * @param source
   * @param target - the target document or a container (if this is a container, the target will be a document with the name of the source)
   * @param transferProperties
   * @return
   */
  public static TransferListener copy(DataPath source, DataPath target, TransferProperties transferProperties) {

    if (Tabulars.isContainer(target)){
      target = target.getChild(source.getName());
    }

    TransferListener transferListener;

    final TableSystem sourceDataSystem = source.getDataSystem();
    if (sourceDataSystem.getClass().equals(target.getDataSystem().getClass())) {
      // same provider (fs or jdbc)
      transferListener = sourceDataSystem.copy(source, target, transferProperties);
    } else {
      // different provider (fs to jdbc or jdbc to fs)
      transferListener = TransferManager.transfer(source, target, transferProperties);
    }

    return transferListener;

  }

  /**
   *
   * @param source - a source document data path
   * @param target - a target document or container (If this is a container, the target document will get the name of the source document)
   * @return
   *
   */
  public static TransferListener copy(DataPath source, DataPath target) {
    return copy(source, target, TransferProperties.of());
  }

  public static TransferListener insert(DataPath source, DataPath target, TransferProperties transferProperties) {
    assert transferProperties != null : "The transfer properties should not be null";

    TransferListener transferListener = null;
    transferProperties.setLoadOperation(TransferLoadOperation.INSERT);
    final TableSystem sourceDataSystem = source.getDataSystem();
    if (sourceDataSystem.equals(target.getDataSystem())) {
      // same provider (fs or jdbc)
      sourceDataSystem.insert(source, target, transferProperties);
    } else {
      // different provider (fs to jdbc or jdbc to fs)
      TransferManager.checkOrCreateTargetStructureFromSource(source, target);
      transferListener = TransferManager.transfer(source, target, transferProperties);
    }
    return transferListener;
  }

  public static TransferListener insert(DataPath source, DataPath target) {
    return insert(source, target, TransferProperties.of());
  }

  /**
   * @param dataPath the data path
   * @return data paths that references the data path primary via a foreign key
   */
  public static List<DataPath> getReferences(DataPath dataPath) {
    return dataPath.getDataSystem().getReferences(dataPath);
  }

  public static void dropOneToManyRelationship(ForeignKeyDef foreignKeyDef) {
    throw new RuntimeException("Not yet implemented");
  }

  public static void copyDataDef(DataPath source, DataPath target) {
    DataDefs.copy(source.getDataDef(), target.getDataDef());
  }
}
