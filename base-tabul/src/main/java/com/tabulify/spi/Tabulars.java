package com.tabulify.spi;

import com.tabulify.DbLoggers;
import com.tabulify.connection.Connection;
import com.tabulify.diff.DataDiffColumn;
import com.tabulify.diff.DataPathDiff;
import com.tabulify.engine.ForeignKeyDag;
import com.tabulify.fs.sql.FsSqlDataPath;
import com.tabulify.fs.textfile.FsTextDataPath;
import com.tabulify.model.Constraint;
import com.tabulify.model.ForeignKeyDef;
import com.tabulify.model.UniqueKeyDef;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.Printer;
import com.tabulify.transfer.*;
import com.tabulify.dag.Dag;
import com.tabulify.exception.ExceptionWrapper;
import com.tabulify.exception.MissingSwitchBranch;
import com.tabulify.java.JavaEnvs;
import com.tabulify.glob.Glob;
import com.tabulify.type.Strings;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;


public class Tabulars {


  public synchronized static Boolean exists(DataPath dataPath) {

    return dataPath.getConnection().getDataSystem().exists(dataPath);

  }


  public static void create(DataPath dataPath) {

    dataPath.getConnection().getDataSystem().create(dataPath, null, null);

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

  public static void drop(DataPath dataPath, DropTruncateAttribute... dropAttributes) {


    drop(List.of(dataPath), dropAttributes);


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
  public static void drop(List<DataPath> dataPaths, DropTruncateAttribute... dropAttributes) {

    dropOrTruncateUtility(DropTruncate.DROP, dataPaths, dropAttributes);

  }

  private static void dropOrTruncateUtility(DropTruncate dropTruncate, List<DataPath> dataPaths, DropTruncateAttribute[] dropAttributes) {

    if (dataPaths.isEmpty()) {
      return;
    }

    /**
     * Batch them by connection
     */
    Map<Connection, List<DataPath>> dataPathsByDataStores = dataPaths
      .stream()
      .collect(
        groupingBy(
          DataPath::getConnection,
          mapping(dp -> dp, toList())
        )
      );

    Set<DropTruncateAttribute> dropAttributeSet = new HashSet<>();
    if (dropAttributes != null) {
      dropAttributeSet = Arrays.stream(dropAttributes).collect(toSet());
    }

    /**
     * Doing the work connection by connection
     */
    for (Connection connection : dataPathsByDataStores.keySet()) {
      List<DataPath> dataPathForConnectionList = dataPathsByDataStores.get(connection);

      DataSystem dataSystem = connection.getDataSystem();

      /**
       * Force Flag implementation
       * Check if the truncated/dropped table
       * have no dependencies or that the dependencies
       * are also in the tables to truncate
       * <p>
       * Note:
       * * Normally, the database enforce this constraint but as Sqlite does not, we do
       * * a list of views created with non-existing tables will fail miserably to get the references
       */
      boolean withForce = dropAttributeSet.contains(DropTruncateAttribute.FORCE);
      boolean ifExists = dropAttributeSet.contains(DropTruncateAttribute.IF_EXISTS);
      try {
        List<DataPath> dataPathToTruncates = ForeignKeyDag.createFromPaths(dataPathForConnectionList).getDropOrdered();
        for (DataPath dataPathToTruncate : dataPathToTruncates) {
          if (ifExists && !Tabulars.exists(dataPathToTruncate)) {
            continue;
          }
          List<ForeignKeyDef> exportedForeignKeys = Tabulars.getReferences(dataPathToTruncate);
          for (ForeignKeyDef exportedForeignKey : exportedForeignKeys) {
            DataPath exportedDataPath = exportedForeignKey.getRelationDef().getDataPath();
            if (!dataPathForConnectionList.contains(exportedDataPath)) {
              if (withForce) {
                Tabulars.dropConstraint(exportedForeignKey);
                DbLoggers.LOGGER_DB_ENGINE.warning("ForeignKey (" + exportedForeignKey.getName() + ") was dropped from the table (" + exportedDataPath + ")");
              } else {
                String msg = Strings.createMultiLineFromStrings(
                  "Unable to " + dropTruncate + " the data resource (" + dataPathToTruncate + ")",
                  "The table (" + exportedDataPath + ") is referencing the table (" + dataPathToTruncate + ") and is not in the tables to " + dropTruncate,
                  "To resolve, this problem you can:",
                  "  * drop the foreign keys referencing the tables to " + dropTruncate + "  with the " + DropTruncateAttribute.FORCE + " flag.",
                  "  * add the primary tables referencing the tables to " + dropTruncate + " with the with-dependencies flag."

                ).toString();
                throw new IllegalArgumentException(msg);

              }
            }
          }
        }
      } catch (Exception e) {
        /**
         * A list of Views created with non-existing tables will fail miserably to get the references
         * We should check the validity of the resource before doing that
         */
        long viewCount = dataPathForConnectionList.stream()
          .filter(s -> s.getMediaType().getSubType().equalsIgnoreCase("view"))
          .count();
        if (viewCount == 0) {
          throw ExceptionWrapper.builder(e, "resource graph building before " + dropTruncate + " failed")
            .setPosition(ExceptionWrapper.ContextPosition.FIRST)
            .buildAsRuntimeException();
        }
        DbLoggers.LOGGER_DB_ENGINE.warning("Error while building the data resources graph with view. Error: " + e.getMessage());
      }

      /**
       * Dropping/Truncating
       */
      List<DataPath> dataPathOrdered;
      switch (dataPathForConnectionList.size()) {
        case 0:
          return;
        case 1:
          // A dag will build the data def, and we may not want it when dropping/truncating only one table
          dataPathOrdered = List.of(dataPathForConnectionList.get(0));
          break;
        default:
          Dag<DataPath> dag = ForeignKeyDag.createFromPaths(dataPathForConnectionList);
          dataPathOrdered = dag.getDropOrdered();
          break;
      }

      switch (dropTruncate) {
        case DROP:
          dataSystem.drop(dataPathOrdered, dropAttributeSet);
          break;
        case TRUNCATE:
          dataSystem.truncate(dataPathOrdered, dropAttributeSet);
          break;
        default:
          throw new MissingSwitchBranch("drop/truncate operation", dropTruncate);
      }
    }
  }

  /**
   * @param sourceDataPath - the records to delete
   * @param targetDataPath - the target where the records are going to be deleted
   */
  public static TransferListener delete(DataPath sourceDataPath, DataPath targetDataPath) {

    return TransferManager
      .builder()
      .setTransferPropertiesSystem(
        TransferPropertiesSystem
          .builder()
          .setOperation(TransferOperation.DELETE)
      )
      .build()
      .createOrder(sourceDataPath, targetDataPath)
      .execute()
      .getTransferListeners()
      .get(0);

  }

  public static void dropIfExists(DataPath dataPath, DropTruncateAttribute... dropAttributes) {

    dropIfExists(List.of(dataPath), dropAttributes);

  }


  /**
   * Drop the table from the database if exist
   * and drop the table from the cache
   */
  public static void dropIfExists(List<DataPath> dataPaths, DropTruncateAttribute... dropAttributes) {

    /**
     * Create new array with the {@link DropTruncateAttribute.IF_EXISTS} flag
     * so that the data path is dropped from any cache even if it does not exist,
     * and we can use the underlying statement
     * (ie drop table if exists for sql) wining a round trip
     */
    DropTruncateAttribute[] dropAttributesWithExists = Arrays.copyOf(dropAttributes, dropAttributes.length + 1);
    dropAttributesWithExists[dropAttributes.length] = DropTruncateAttribute.IF_EXISTS;

    // A hack to avoid building the data def
    // Because the getDropOrderedTables will build the dependency
    if (dataPaths.size() == 1) {
      DataPath dataPath = dataPaths.get(0);
      Tabulars.drop(dataPath, dropAttributesWithExists);
      return;
    }

    /**
     * Multiple drop, we need a dag
     */
    List<DataPath> dropOrdered = ForeignKeyDag.createFromPaths(dataPaths).getDropOrdered();
    for (DataPath dataPath : dropOrdered) {
      Tabulars.drop(dataPath, dropAttributesWithExists);
    }


  }

  /**
   * Postgres does not allow to truncate a table referenced by a foreign key
   * if the two tables are not in the same statement
   * <a href="https://www.postgresql.org/docs/current/sql-truncate.html">Sql Truncate</a>
   */
  public static void truncate(List<DataPath> dataPaths, DropTruncateAttribute... dropAttributes) {

    dropOrTruncateUtility(DropTruncate.TRUNCATE, dataPaths, dropAttributes);

  }

  public static void truncate(DataPath dataPath) {
    truncate(List.of(dataPath));
  }


  /**
   * Print the data of a table
   *
   * @param dataPath - the data path to print
   */
  public static void print(DataPath dataPath) {

    Printer.PrintBuilder builder = Printer.builder();
    String standardColorsColumn = DataPathDiff.DIFF_COLUMN_PREFIX + DataDiffColumn.COLORS.toKeyNormalizer().toSqlCase();
    if (JavaEnvs.isRunningInTerminal() && dataPath.getOrCreateRelationDef().hasColumn(standardColorsColumn)) {
      builder.setColorsColumnName(standardColorsColumn);
    }
    builder.
      build()
      .print(dataPath);


  }

  public static void print(List<DataPath> dataPaths) {

    for (DataPath dataPath : dataPaths) {
      Tabulars.print(dataPath);
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
   * @return if the data path locate a document
   * <p>
   * The counterpart is {@link #isContainer(DataPath)}
   */
  public static boolean isDocument(DataPath dataPath) {
    return dataPath.getConnection().getDataSystem().isDocument(dataPath);
  }

  public static void create(List<DataPath> dataPaths) {
    Dag<DataPath> dag = ForeignKeyDag.createFromPaths(dataPaths);
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

    if (!Tabulars.exists(dataPath)) {
      if (dataPath.getConnection().getTabular().isStrictExecution()) {
        throw new RuntimeException("The data path (" + dataPath + ") does not exist, we can get its children");
      }
      return Collections.emptyList();
    }
    if (Tabulars.isDocument(dataPath)) {
      throw new RuntimeException("The data path (" + dataPath + ") is a document (" + dataPath.getMediaType() + "), it has therefore no children");
    }
    return dataPath.getConnection()
      .getDataSystem()
      .getChildrenDataPath(dataPath)
      .stream()
      .sorted()
      .collect(Collectors.toList());

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
    return dataPath.getConnection().getDataSystem().getContentAsString(dataPath);
  }

  /**
   * Move a source document to a target document
   * If the document is:
   * * on the same data store, it's a rename operation,
   * * not on the same data store, it's a transfer and a `delete` from the source
   *
   * @param source - the source to move
   * @param target - a target data path container or document
   *               If the target is a container, the target will have the name of the source
   * @return a {@link TransferListenerStream} or null if it was no transfer
   */
  public static TransferListener move(DataPath source, DataPath target, TransferResourceOperations... targetDataOperations) {

    return TransferManager
      .builder()
      .setTransferPropertiesSystem(
        TransferPropertiesSystem
          .builder()
          .setOperation(TransferOperation.COPY)
          .setSourceOperations(TransferResourceOperations.DROP)
          .setTargetOperations(targetDataOperations)
      )
      .build()
      .createOrder(source, target)
      .execute()
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
   * @return a transfer listener
   */
  public static TransferListener copy(DataPath source, DataPath target, TransferResourceOperations... targetDataOperations) {

    return TransferManager
      .builder()
      .setTransferPropertiesSystem(
        TransferPropertiesSystem
          .builder()
          .setOperation(TransferOperation.COPY)
          .setTargetOperations(targetDataOperations)
      )
      .build()
      .createOrder(source, target)
      .execute()
      .getTransferListeners()
      .get(0);

  }


  public static TransferListener insert(DataPath source, DataPath target) {

    return TransferManager
      .builder()
      .setTransferPropertiesSystem(
        TransferPropertiesSystem
          .builder()
          .setOperation(TransferOperation.INSERT)
      )
      .build()
      .createOrder(source, target)
      .execute()
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
   */
  public static void dropConstraint(Constraint constraint) {
    if (constraint != null) {
      constraint.getRelationDef().getDataPath().getConnection().getDataSystem().dropConstraint(constraint);
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


  public static TransferListener transfer(DataPath sourceDataPath, DataPath targetDataPath, TransferPropertiesSystem.TransferPropertiesSystemBuilder transferPropertiesCross) {
    return TransferManager
      .builder()
      .setTransferPropertiesSystem(transferPropertiesCross)
      .build()
      .createOrder(sourceDataPath, targetDataPath)
      .execute()
      .getTransferListeners()
      .get(0);
  }


  public static TransferListener upsert(DataPath sourceDataPath, DataPath targetDataPath, TransferResourceOperations... targetDataOperations) {
    return TransferManager
      .builder()
      .setTransferPropertiesSystem(
        TransferPropertiesSystem
          .builder()
          .setOperation(TransferOperation.UPSERT)
          .setTargetOperations(targetDataOperations)
      )
      .build()
      .createOrder(sourceDataPath, targetDataPath)
      .execute()
      .getTransferListeners()
      .get(0);
  }

  public static TransferListener update(DataPath sourceDataPath, DataPath targetDataPath, TransferPropertiesSystem.TransferPropertiesSystemBuilder transferProperties) {
    transferProperties.setOperation(TransferOperation.UPDATE);
    return TransferManager
      .builder()
      .setTransferPropertiesSystem(transferProperties)
      .build()
      .createOrder(sourceDataPath, targetDataPath)
      .execute()
      .getTransferListeners()
      .get(0);
  }

  public static TransferListener update(DataPath sourceDataPath, DataPath targetDataPath, TransferResourceOperations... targetDataOperations) {
    return update(sourceDataPath, targetDataPath, TransferPropertiesSystem
      .builder()
      .setTargetOperations(targetDataOperations)
    );

  }

  /**
   * @return true if the data resource is a runtime
   */
  public static boolean isRuntime(DataPath dataPath) {
    return dataPath.isRuntime();
  }


  /**
   * A free schema form structure accepts any number of columns on insertion
   * Free from data path will:
   * * not see their schema tested
   * * see their schema created on the fly
   * There is:
   * * {@link FsTextDataPath}
   * * {@link FsSqlDataPath}
   * * by default 2 (token type and sql) but this is the structure when reading the file,
   * * not writing that will accept generally only the sql
   */
  public static boolean isFreeSchemaForm(DataPath dataPath) {
    return dataPath.getClass().equals(FsTextDataPath.class) || dataPath.getClass().equals(FsSqlDataPath.class);
  }


  /**
   * Utility function to insert lines
   */
  public static void writeLines(DataPath dataPath, String input) {
    if (input == null) {
      return;
    }
    try (InsertStream insertStream = dataPath.getInsertStream()) {
      for (String line : input.split("\\r?\\n")) {
        insertStream.insert(line);
      }
    }
  }
}
