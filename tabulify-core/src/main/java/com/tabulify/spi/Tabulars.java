package com.tabulify.spi;

import com.tabulify.DbLoggers;
import com.tabulify.connection.Connection;
import com.tabulify.engine.ForeignKeyDag;
import com.tabulify.model.Constraint;
import com.tabulify.model.ForeignKeyDef;
import com.tabulify.model.UniqueKeyDef;
import com.tabulify.stream.SelectStream;
import com.tabulify.stream.Streams;
import com.tabulify.transfer.*;
import net.bytle.dag.Dag;
import net.bytle.regexp.Glob;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class Tabulars {


  public synchronized static Boolean exists(DataPath dataPath) {

    return dataPath.getConnection().getDataSystem().exists(dataPath);

  }


  public static void create(DataPath dataPath) {

    dataPath.getConnection().getDataSystem().create(dataPath);

  }

  /**
   * @param dataPaths - a list of data path
   * @return return a independent set of data path (ie independent of foreign key outside of the set)
   * <p>
   * (ie delete the foreign keys of a table if the foreign table is not part of the set)
   */
  public static List<DataPath> atomic(List<DataPath> dataPaths) {
    for (DataPath dataPath : dataPaths) {
      List<ForeignKeyDef> foreignKeys = dataPath.getOrCreateRelationDef().getForeignKeys();
      for (ForeignKeyDef foreignKeyDef : foreignKeys) {
        if (!(dataPaths.contains(foreignKeyDef.getForeignPrimaryKey().getRelationDef().getDataPath()))) {
          dataPath.getOrCreateRelationDef().deleteForeignKey(foreignKeyDef);
        }
      }
    }
    return dataPaths;
  }

  /**
   * Create all data object if they don't exist
   * taking into account the foreign key constraints
   * <p>
   */
  public static void createIfNotExist(List<DataPath> dataPaths) {

    Dag<DataPath> dag = ForeignKeyDag.createFromPaths(dataPaths);
    List<DataPath> orderedDataPaths = dag.getCreateOrdered();
    for (DataPath dataPath : orderedDataPaths) {
      createIfNotExist(dataPath);
    }

  }

  public static boolean isContainer(DataPath dataPath) {

    return dataPath.getConnection().getDataSystem().isContainer(dataPath);

  }

  /**
   * Create the data resource in the system if it doesn't exist
   */
  public static void createIfNotExist(DataPath dataPath) {

    if (exists(dataPath)) {
      DbLoggers.LOGGER_DB_ENGINE.fine("The data resource (" + dataPath + ") already exist.");
      return;
    }
    create(dataPath);


  }

  public static void drop(DataPath dataPath, DataPath... dataPaths) {

    if (dataPaths.length != 0) {

      // Create one list
      List<DataPath> allDataPaths = new ArrayList<>();
      allDataPaths.add(dataPath);
      allDataPaths.addAll(Arrays.asList(dataPaths));

      // A dag will build the data def, and we may not want it when dropping only one table
      Dag<DataPath> dag = ForeignKeyDag.createFromPaths(allDataPaths);
      List<DataPath> dropOrdered = dag.getDropOrdered();
      for (DataPath orderedDataPath : dropOrdered) {
        dataPath.getConnection().getDataSystem().drop(orderedDataPath);
      }
      return;

    }

    /**
     * Needed when we manipulate only one table
     * (If we delete several at once, we need to update the data def (data structure)
     * and if we just want to drop it, we may get side effect due
     * to update of the metadata
     */
    dataPath.getConnection().getDataSystem().drop(dataPath);


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

    if (dataPaths.isEmpty()) {
      return;
    }

    DataPath[] moreDataPath = {};
    if (dataPaths.size() > 1) {
      moreDataPath = dataPaths.subList(1, dataPaths.size()).toArray(new DataPath[0]);
    }
    drop(dataPaths.get(0), moreDataPath);


  }

  /**
   * Suppress all rows of the table
   *
   * @param dataPath - the tableDef where to suppress all rows
   */
  public static void delete(DataPath dataPath) {

    dataPath.getConnection().getDataSystem().delete(dataPath);

  }

  public static TransferListener delete(DataPath sourceDataPath, DataPath targetDataPath) {

    return TransferManager
      .create()
      .setTransferProperties(
        TransferProperties
          .create()
          .setOperation(TransferOperation.DELETE)
      )
      .addTransfer(sourceDataPath, targetDataPath)
      .run()
      .getTransferListeners()
      .get(0);

  }

  public static void dropIfExists(DataPath... dataPaths) {

    dropIfExists(Arrays.asList(dataPaths));

  }


  /**
   * Drop the table from the database if exist
   * and drop the table from the cache
   */
  public static void dropIfExists(List<DataPath> dataPaths) {

    // A hack to avoid building the data def
    // Because the getDropOrderedTables will build the dependency
    if (dataPaths.size() == 1) {
      DataPath dataPath = dataPaths.get(0);
      dataPath.getConnection().getDataSystem().dropIfExist(dataPath);
      return;
    }

    /**
     * Multiple drop, we need a dag
     */
    List<DataPath> dropOrdered = ForeignKeyDag.createFromPaths(dataPaths).getDropOrdered();
    for (DataPath dataPath : dropOrdered) {
      dataPath.getConnection().getDataSystem().dropIfExist(dataPath);
    }


  }

  public static void truncate(DataPath dataPath) {
    dataPath.getConnection().getDataSystem().truncate(dataPath);
  }


  /**
   * Print the data of a table
   *
   * @param dataPath
   */
  public static void print(DataPath dataPath) {

    try (SelectStream tableOutputStream = dataPath.getSelectStream()) {
      Streams.print(tableOutputStream);
    } catch (SelectException e) {
      throw new RuntimeException(e);
    }

  }


  public static List<DataPath> move(List<DataPath> sources, DataPath target) {

    List<DataPath> targetDataPaths = new ArrayList<>();
    for (DataPath sourceDataPath : ForeignKeyDag.createFromPaths(sources).getCreateOrdered()) {
      DataPath targetDataPath = target.getConnection().getDataPath(sourceDataPath.getName());
      Tabulars.move(sourceDataPath, targetDataPath);
      targetDataPaths.add(targetDataPath);
    }

    return targetDataPaths;

  }


  public static boolean isEmpty(DataPath queue) {
    return queue.getConnection().getDataSystem().isEmpty(queue);
  }


  /**
   * @param dataPath
   * @return if the data path locate a document
   * <p>
   * The counter part is {@link #isContainer(DataPath)}
   */
  public static boolean isDocument(DataPath dataPath) {
    return dataPath.getConnection().getDataSystem().isDocument(dataPath);
  }

  public static void create(List<DataPath> dataPaths) {
    Dag dag = ForeignKeyDag.createFromPaths(dataPaths);
    dataPaths = dag.getCreateOrdered();
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
    return dataPath.getConnection().getDataSystem().getChildrenDataPath(dataPath);

  }

  /**
   * @param dataPath    - a parent/container dataPath
   * @param globPattern -  a glob pattern
   * @return the children data path of the parent that matches the glob pattern
   */
  public static List<DataPath> getChildren(DataPath dataPath, String globPattern) {
    final Glob glob = Glob.createOf(globPattern);
    return getChildren(dataPath)
      .stream()
      .filter(s -> glob.matches(s.getName()))
      .collect(Collectors.toList());
  }


  /**
   * @param one  - the primary key table
   * @param many - the foreign key table
   * @return the dropped foreign keys
   */
  public static List<ForeignKeyDef> dropOneToManyRelationship(DataPath one, DataPath many) {

    List<ForeignKeyDef> foreignKeyDefs = one.getOrCreateRelationDef().getForeignKeys().stream()
      .filter(fk -> fk.getForeignPrimaryKey().getRelationDef().getDataPath().equals(many))
      .collect(Collectors.toList());

    foreignKeyDefs.stream()
      .forEach(fk -> {
        fk.getRelationDef().getDataPath().getConnection().getDataSystem().dropConstraint(fk);
      });

    return foreignKeyDefs;

  }

  /**
   * @param dataPath
   * @return the content of a data path in a string format
   */
  public static String getString(DataPath dataPath) {
    return dataPath.getConnection().getDataSystem().getString(dataPath);
  }

  /**
   * Move a source document to a target document
   * If the document is:
   * * on the same data store, it's a rename operation,
   * * not on the same data store, it's a transfer and a delete from the source
   *
   * @param source - the source to move
   * @param target - a target data path container or document
   *               If the target is a container, the target will have the name of the source
   * @return a {@link TransferListenerStream} or null if it was no transfer
   */
  public static TransferListener move(DataPath source, DataPath target, TransferResourceOperations... targetDataOperations) {

    return TransferManager
      .create()
      .setTransferProperties(
        TransferProperties
          .create()
          .setOperation(TransferOperation.MOVE)
          .addTargetOperations(targetDataOperations)
      )
      .addTransfer(source, target)
      .run()
      .getTransferListeners()
      .get(0);

  }

  private static boolean sameDataSystem(DataPath source, DataPath target) {
    return source.getConnection().getDataSystem().getClass().equals(target.getConnection().getDataSystem().getClass());
  }

  /**
   * @param source               - a source document data path
   * @param target               - a target document or container (If this is a container, the target document will get the name of the source document)
   * @param targetDataOperations - the target data operations
   * @return
   */
  public static TransferListener copy(DataPath source, DataPath target, TransferResourceOperations... targetDataOperations) {

    return TransferManager
      .create()
      .setTransferProperties(
        TransferProperties
          .create()
          .setOperation(TransferOperation.COPY)
          .addTargetOperations(targetDataOperations)
      )
      .addTransfer(source, target)
      .run()
      .getTransferListeners()
      .get(0);

  }


  public static TransferListener insert(DataPath source, DataPath target) {

    return TransferManager
      .create()
      .setTransferProperties(
        TransferProperties
          .create()
          .setOperation(TransferOperation.INSERT)
      )
      .addTransfer(source, target)
      .run()
      .getTransferListeners()
      .get(0);
  }


  /**
   * @param dataPath the data path
   * @return the foreign keys that references the data path primary key (known also as exported keys)
   */
  public static List<ForeignKeyDef> getReferences(DataPath dataPath) {
    return dataPath.getConnection().getDataSystem().getForeignKeysThatReference(dataPath);
  }

  public static void dropOneToManyRelationship(ForeignKeyDef foreignKeyDef) {
    throw new UnsupportedOperationException("Not yet implemented");
  }


  /**
   * Drop a constraint
   *
   * @param constraint
   */
  public static void dropConstraint(Constraint constraint) {
    if (constraint != null) {
      constraint.getRelationDef().getDataPath().getConnection().getDataSystem().dropConstraint(constraint);
    }
  }

  /**
   * Postgres does not allow to truncate a table referenced by a foreign key
   * if the two tables are not in the same statement
   * <a href="https://www.postgresql.org/docs/current/sql-truncate.html">Sql Truncate</a>
   *
   * @param dataPaths
   */
  public static void truncate(List<DataPath> dataPaths) {
    if (dataPaths.size() == 0) {
      throw new IllegalStateException("The number of data paths is zero, we can't truncate them");
    }
    List<Connection> connections = dataPaths.stream().map(DataPath::getConnection).distinct().collect(Collectors.toList());
    if (connections.size() != 1) {
      throw new IllegalStateException("We found more than one datastore (" + connections.stream().map(Connection::getName).collect(Collectors.joining(", ")) + ". This function does not support to truncate tables from two different datastores.");
    }
    dataPaths.get(0).getConnection().getDataSystem().truncate(dataPaths);
  }


  /**
   * Drop a target data path and all the foreign keys that
   * reference it
   *
   * @param dataPath
   */
  public static void dropForceIfExists(DataPath dataPath) {

    if (exists(dataPath)) {
      dataPath.getConnection().getDataSystem().dropForce(dataPath);
    }

  }

  /**
   * Drop all constraint of the data path
   *
   * @param dataPath
   */
  public static void dropForceAllConstraints(DataPath dataPath) {

    /**
     * Drop the primary key
     */
    dropForcePrimaryKeyConstraint(dataPath);

    /**
     * Drop the not null constraints
     */
    Tabulars.dropNotNull(dataPath);

    /**
     * Drop the uniques keys
     */
    for (UniqueKeyDef uniqueKey : dataPath.getRelationDef().getUniqueKeys()) {
      Tabulars.dropConstraint(uniqueKey);
    }
  }

  public static void dropNotNull(DataPath dataPath) {
    dataPath.getConnection().getDataSystem().dropNotNullConstraint(dataPath);
  }


  public static void dropForcePrimaryKeyConstraint(DataPath dataPath) {
    /**
     * Drop the foreign key that references the primary key of the data path
     */
    for (ForeignKeyDef reference : Tabulars.getReferences(dataPath)) {
      Tabulars.dropConstraint(reference);
    }
    Tabulars.dropConstraint(dataPath.getOrCreateRelationDef().getPrimaryKey());
  }


  public static TransferListener transfer(DataPath sourceDataPath, DataPath targetDataPath, TransferProperties transferProperties) {
    return TransferManager
      .create()
      .setTransferProperties(transferProperties)
      .addTransfer(sourceDataPath, targetDataPath)
      .run()
      .getTransferListeners()
      .get(0);
  }


  public static TransferListener upsert(DataPath sourceDataPath, DataPath targetDataPath, TransferResourceOperations... targetDataOperations) {
    return TransferManager
      .create()
      .setTransferProperties(
        TransferProperties
          .create()
          .setOperation(TransferOperation.UPSERT)
          .addTargetOperations(targetDataOperations)
      )
      .addTransfer(sourceDataPath, targetDataPath)
      .run()
      .getTransferListeners()
      .get(0);
  }

  public static TransferListener update(DataPath sourceDataPath, DataPath targetDataPath, TransferResourceOperations... targetDataOperations) {
    return TransferManager
      .create()
      .setTransferProperties(
        TransferProperties
          .create()
          .setOperation(TransferOperation.UPDATE)
          .addTargetOperations(targetDataOperations)
      )
      .addTransfer(sourceDataPath, targetDataPath)
      .run()
      .getTransferListeners()
      .get(0);
  }

  /**
   * @param dataPath
   * @return true if the data resource is a script
   */
  public static boolean isScript(DataPath dataPath) {
    return dataPath.isScript();
  }

  public static void execute(DataPath dataPath) {
    dataPath.getConnection().getDataSystem().execute(dataPath);
  }


}
