package com.tabulify.transfer;

import com.tabulify.DbLoggers;
import com.tabulify.exception.DataResourceNotEmptyException;
import com.tabulify.fs.FsDataPath;
import com.tabulify.fs.textfile.FsTextDataPath;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.ColumnDefBase;
import com.tabulify.model.PrimaryKeyDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.Tabulars;
import net.bytle.exception.InternalException;
import net.bytle.exception.NoColumnException;
import net.bytle.log.Log;
import net.bytle.log.Logs;
import net.bytle.type.MapBiDirectional;
import net.bytle.type.Strings;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A class that associate
 * * a source data path
 * * a target data path
 * * and all {@link TransferProperties properties}
 * <p>
 * The function has also transfer utility function.
 * <p>
 * The get functions perform also check when they expect
 * that you are building a statement (For instance {@link #getSourceColumnsInUpdateSetClause()}
 */
public class TransferSourceTarget {

  public static final Log LOGGER = Logs.createFromClazz(TransferSourceTarget.class);

  private final DataPath target;
  private final DataPath source;
  private final TransferProperties transferProperties;


  public TransferSourceTarget(DataPath sourceDataPath, DataPath targetDataPath, TransferProperties transferProperties) {
    if (sourceDataPath != null) {
      this.source = sourceDataPath;
    } else {
      /**
       * If the source is not defined, we expect the same structure than the target
       */
      this.source = targetDataPath;
    }
    this.target = targetDataPath;
    this.transferProperties = transferProperties;
  }

  public static TransferSourceTarget create(DataPath source, DataPath target) {
    return create(source, target, TransferProperties.create());
  }

  public static TransferSourceTarget create(DataPath source, DataPath target, TransferProperties transferProperties) {
    return new TransferSourceTarget(source, target, transferProperties);
  }

  public DataPath getSourceDataPath() {
    return source;
  }

  public DataPath getTargetDataPath() {
    return target;
  }

  @Override
  public String toString() {
    return " " + source + " > " + target + " ";
  }


  /**
   * @return a {@link MapBiDirectional bidirectional map} of the source and target
   * by the mapping strategy (position or name)
   * @throws IllegalArgumentException if the column mapping given is not good
   */
  public Map<Integer, Integer> getTransferColumnMapping() {
    List<? extends ColumnDef> sourceColumns = source.getOrCreateRelationDef().getColumnDefs();
    Map<Integer, Integer> columnMappings;
    TransferColumnMapping columnMappingMethod = transferProperties.getColumnMappingMethod();
    switch (columnMappingMethod) {
      case POSITION:
        /**
         * one on one (1=1, 2=2, ...)
         * or 1=1 2=1 (transfer to a text file) {@link com.tabulify.fs.textfile.FsTextDataPath}
         */
        columnMappings = new HashMap<>();
        int targetColumnsSize = target.getOrCreateRelationDef().getColumnsSize();
        for (ColumnDef sourceColumn : sourceColumns) {
          Integer sourceColumnPosition = sourceColumn.getColumnPosition();
          columnMappings.put(sourceColumnPosition, sourceColumnPosition);
          if (sourceColumnPosition > targetColumnsSize) {
            if (!(target instanceof FsTextDataPath)) {
              throw new RuntimeException("The target column with the position (" + sourceColumnPosition + ") does not exist. The target (" + target + ") has only " + targetColumnsSize + " columns.");
            }
            columnMappings.put(sourceColumnPosition, targetColumnsSize);
          }
        }
        break;
      case NAME:
        // You can't map one column to another
        columnMappings = new MapBiDirectional<>();
        for (ColumnDef sourceColumn : sourceColumns) {
          Integer columnPosition = sourceColumn.getColumnPosition();
          ColumnDef targetColumn;
          try {
            targetColumn = target.getOrCreateRelationDef().getColumnDef(sourceColumn.getColumnName());
          } catch (NoColumnException e) {
            throw new RuntimeException("Error during the mapping of the source and target columns (by name), a column with the name (" + sourceColumn.getColumnName() + ") could not be found in the target (" + target + ")");
          }
          Integer targetColumnPosition = targetColumn.getColumnPosition();
          columnMappings.put(columnPosition, targetColumnPosition);
        }
        break;
      case MAP_BY_POSITION:
        columnMappings = transferProperties.getColumnMappingByMapPosition();
        // Check that the columns are existing
        int sourceSize = source.getRelationDef().getColumnsSize();
        int targetSize = target.getRelationDef().getColumnsSize();
        for (Map.Entry<Integer, Integer> columnMapping : columnMappings.entrySet()) {
          Integer sourceColumnPosition = columnMapping.getKey();
          if (sourceColumnPosition > sourceSize) {
            throw new IllegalArgumentException("The column mapping given for the transfer (" + this + ") is not good. The source column (" + sourceColumnPosition + ") does not exists");
          }
          Integer targetColumnPosition = columnMapping.getValue();
          if (targetColumnPosition > targetSize) {
            throw new IllegalArgumentException("The column mapping given for the transfer (" + this + ") is not good. The target column (" + targetColumnPosition + ") does not exists");
          }
        }
        break;
      case MAP_BY_NAME:
        // You can't map one column to another
        columnMappings = new MapBiDirectional<>();
        MapBiDirectional<String, String> columnMappingsByName = transferProperties.getColumnMappingByMapNamed();
        // Check that the columns are existing and transform the map in a positional map
        for (Map.Entry<String, String> columnMapping : columnMappingsByName.entrySet()) {
          String sourceColumnName = columnMapping.getKey();
          ColumnDef sourceColumn;
          try {
            sourceColumn = source.getRelationDef().getColumnDef(sourceColumnName);
          } catch (NoColumnException e) {
            throw new IllegalArgumentException("The column mapping (" + columnMapping + ") given for the transfer (" + this + ") is not good. The source column (" + sourceColumnName + ") does not exists in the source (" + source + ")");
          }
          String targetColumnName = columnMapping.getValue();
          ColumnDef targetColumn;
          try {
            targetColumn = target.getRelationDef().getColumnDef(targetColumnName);
          } catch (NoColumnException e) {
            throw new IllegalArgumentException("The column mapping (" + columnMapping + ") given for the transfer (" + this + ") is not good. The target column (" + targetColumnName + ") does not exists in the target (" + target + ")");
          }
          columnMappings.put(sourceColumn.getColumnPosition(), targetColumn.getColumnPosition());
        }
        break;
      default:
        throw new RuntimeException("Mapping method (" + columnMappingMethod + ") was not implemented. This is a internal bug.");
    }
    return columnMappings;

  }


  /**
   * @return a list of source column position that corresponds to the placeholder in the statement order
   * <p>
   * This function is used during loading to retrieve the objects in a statement order from the source
   * The id is the {@link ColumnDefBase#getColumnPosition() column position}
   */
  public List<Integer> getSourceColumnPositionInStatementOrder() {


    if (transferProperties.getOperation() == TransferOperation.UPDATE) {

      /**
       * The column in the set clause
       */
      List<Integer> sourceColumnPositionInStatementOrder = new ArrayList<>();
      getSourceColumnsInUpdateSetClause().forEach(c -> sourceColumnPositionInStatementOrder.add(c.getColumnPosition()));
      /**
       * Followed by the unique columns in the where clause
       */
      getSourceUniqueColumnsForTarget().forEach(c -> sourceColumnPositionInStatementOrder.add(c.getColumnPosition()));
      return sourceColumnPositionInStatementOrder;

    } else if (transferProperties.getOperation() == TransferOperation.DELETE) {

      /**
       * The unique columns in the where clause
       */
      List<Integer> sourceColumnPositionInStatementOrder = new ArrayList<>();
      getSourceUniqueColumnsForTarget().forEach(c -> sourceColumnPositionInStatementOrder.add(c.getColumnPosition()));
      return sourceColumnPositionInStatementOrder;

    } else if (transferProperties.getOperation() == TransferOperation.INSERT || transferProperties.getOperation() == TransferOperation.UPSERT) {

      List<Integer> sourceColumnPositionInStatementOrder = new ArrayList<>();
      getSourceColumnsInInsertStatement().forEach(c -> sourceColumnPositionInStatementOrder.add(c.getColumnPosition()));
      return sourceColumnPositionInStatementOrder;

    } else {
      /**
       * Code that was created when we wanted to have a mapping by position and not by name
       *
       */
      Map<Integer, Integer> columnMapping = getTransferColumnMapping();
      return columnMapping
        .values()
        .stream()
        .sorted()
        .map(v -> {
          if (columnMapping instanceof MapBiDirectional) {
            return ((MapBiDirectional<Integer, Integer>) columnMapping).getKey(v);
          }
          return columnMapping
            .entrySet()
            .stream()
            .filter(e -> e.getValue().equals(v))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElseThrow();
        })
        .collect(Collectors.toList());
    }

  }


  /**
   * Before a copy/move operations the target
   * table should exist.
   * <p>
   * If the target table:
   * - does not exist, creates the target data resources from the source
   * - exist, control that the column definition is the same
   * <p>
   * <p>
   * If the transfer has multiple transfer, the {@link TransferManager}
   * should be aware that this operation should be performed
   * once by target and pass different {@link TransferProperties}
   * with different {@link TransferResourceOperations}
   * ie you don't want to truncate or delete each time
   *
   * @param transferListener -
   * @param createTarget     - a switch to say if the function should create the table
   *                         (it was introduced to be able to support the `create table as` sql statment.
   *                         If this parameter is false, no creation and table check is performed if the target does not exist
   */
  public void targetPreOperationsAndCheck(TransferListener transferListener, boolean createTarget) {


    /**
     * TODO: Note that the DROP and REPLACE pre-operation
     *   give us a problem on stream run from a pipeline
     *   because they will run each time deleting the data
     *   from now, we have set the {@link TransferStep} to
     *   Accumulator to resolve this problem, but we may not need them
     */
    if (this.getTransferProperties().getRunPreDataOperation()) {

      /**
       * Replace Pre-operations
       * Create is down in the next step
       */
      if (transferProperties.getTargetOperations().contains(TransferResourceOperations.REPLACE)) {
        if (Tabulars.exists(target)) {
          Tabulars.drop(target);
          LOGGER.info("The target data operation (" + TransferResourceOperations.DROP + ") was executed against the target (" + target + ") because of the (" + TransferResourceOperations.REPLACE + ") target operation");
          transferListener.addTargetOperation(TransferResourceOperations.DROP);
        }

      }


      if (!createTarget) {
        // in case the data system create itself the target
        // we just stop here
        // (`create table as` statement in sql for instance)
        return;
      }

      /**
       * Move does not need to create the target
       */
      if (transferProperties.getOperation() == TransferOperation.MOVE) {
        // a move don't create any target
        return;
      }

      /**
       * Target structure creation if needed
       */
      final Boolean targetExists = Tabulars.exists(target);
      if (!targetExists) {
        if (
          (transferProperties.getTargetOperations().contains(TransferResourceOperations.CREATE)
            || transferProperties.getTargetOperations().contains(TransferResourceOperations.REPLACE))
        ) {
          this.createTarget(transferListener);
        } else {
          throw new RuntimeException(
            Strings.createMultiLineFromStrings(
              "The target data resource (" + target + ") does not exist and the " + TransferResourceOperations.CREATE + " data operation is not present.",
              "Create the target or add the data operation (" + TransferResourceOperations.CREATE + ") on the target."
            ).toString());
        }
      }

      /**
       * The target exists at this point
       */

      /**
       * Truncate Pre-operations
       */
      if (transferProperties.getTargetOperations().contains(TransferResourceOperations.TRUNCATE)) {
        Tabulars.truncate(target);
        transferListener.addTargetOperation(TransferResourceOperations.TRUNCATE);
        LOGGER.info("The target data operation (" + TransferResourceOperations.TRUNCATE + ") was executed against the target (" + target + ")");
      }

    }


    /**
     * Empty check (Target should be empty for a copy, move operation if there is no truncate, replace)
     */
    if (
      (transferProperties.getOperation() == TransferOperation.COPY ||
        transferProperties.getOperation() == TransferOperation.MOVE)
        &&
        !(
          transferProperties.getTargetOperations().contains(TransferResourceOperations.TRUNCATE)
            || transferProperties.getTargetOperations().contains(TransferResourceOperations.REPLACE)
        )
    ) {
      if (!Tabulars.isEmpty(target)) {
        throw new DataResourceNotEmptyException("The target data (" + target + ") is not empty. In a copy operation, the target resource should be empty or one of the following target operation (" + TransferResourceOperations.TRUNCATE + ", " + TransferResourceOperations.REPLACE + ") should be present");
      }
    }


    /**
     * Data structure checks
     */
    if (target.getOrCreateRelationDef().getColumnsSize() == 0) {
      throw new InternalException("Internal Error: The target (" + target + ") does not have any columns.");
    }

    /**
     * Move and copy should have the same data structure (same number of columns)
     */
    TransferOperation loadOperation = transferProperties.getOperation();
    if (loadOperation == null) {
      throw new RuntimeException("Internal error, the default load operation is null. It should have already been set by the program.");
    }
    if (loadOperation.requireSameStructureBetweenSourceAndTarget()) {
      TransferColumnMapping columnMappingMethod = transferProperties.getColumnMappingMethod();
      switch (columnMappingMethod) {
        case NAME:
        case POSITION:
          break;
        default:
          throw new RuntimeException("The load operation (" + loadOperation + ") requires to have the same columns. Therefore, you can't use the column mapping (" + columnMappingMethod + "), use the column mapping by name instead or change the load operation to upsert, append, insert");
      }

      for (ColumnDef columnDef : source.getOrCreateRelationDef().getColumnDefs()) {
        try {
          switch (columnMappingMethod) {
            case NAME:
              target.getOrCreateRelationDef().getColumnDef(columnDef.getColumnName());
              break;
            case POSITION:
              target.getOrCreateRelationDef().getColumnDef(columnDef.getColumnPosition());
              break;
            default:
              throw new InternalException("The load operation (" + loadOperation + ") should have a branch in this switch");
          }
        } catch (NoColumnException e) {
          String message = "Unable to " + loadOperation + " the data unit (" + source + ") because it exists already in the target location (" + target + ") with a different structure" +
            " (The source column (" + columnDef.getColumnName() + "," + columnDef.getColumnPosition() + ") was not found in the target data unit)";
          DbLoggers.LOGGER_DB_ENGINE.severe(message);
          throw new RuntimeException(message);
        }
      }
    }

    /**
     * Column Mapping check
     * This function will check that the {@link #getTransferColumnMapping() column mapping}
     * More particularly that the target data type must be able to receive the source data
     * Log on an info level because the cast mechanism can transform it before loading.
     * See SqlDataStore#toSqlObject that uses {@link net.bytle.type.Casts#cast(Object, Class)}
     */
    Map<Integer, Integer> columnMapping = getTransferColumnMapping();
    columnMapping.entrySet().forEach(c -> {
      ColumnDef sourceColumn = source.getOrCreateRelationDef().getColumnDef(c.getKey());
      ColumnDef targetColumn = target.getOrCreateRelationDef().getColumnDef(c.getValue());
      if (sourceColumn.getDataType().getTypeCode() != targetColumn.getDataType().getTypeCode()) {
        String message = Strings.createMultiLineFromStrings(
          "There is a potential problem with a data loading mapping between two columns",
          "The problem is on the mapping (" + c + ") between the source column (" + sourceColumn + ") and the target column (" + targetColumn + ")",
          "where the source data type (" + sourceColumn.getDataType().getSqlName() + ") is different than the target data type (" + targetColumn.getDataType().getSqlName() + ")."
        ).toString();
        LOGGER.fine(message);
      }
    });


  }


  void createTarget(TransferListener transferListener) {

    /**
     * example
     * File: If this for instance, the creation of a file, the file may not exist
     * and have no content and therefore no structure
     */
    TransferColumnMapping columnMappingMethod = transferProperties.getColumnMappingMethod();
    switch (columnMappingMethod) {
      case MAP_BY_NAME:
        MapBiDirectional<String, String> namedMaps = transferProperties.getColumnMappingByMapNamed();
        for (Map.Entry<String, String> namedMap : namedMaps.entrySet()) {
          ColumnDef sourceColumn;
          String key = namedMap.getKey();
          try {
            sourceColumn = source.getRelationDef().getColumnDef(key);
          } catch (NoColumnException e) {
            throw new IllegalArgumentException("The column at the position (" + key + ") for the source resource (" + source + ") does not exists");
          }
          String targetColumnName = namedMap.getValue();
          target.getOrCreateRelationDef()
            .addColumn(
              targetColumnName,
              sourceColumn.getDataType().getTypeCode(),
              sourceColumn.getPrecision(),
              sourceColumn.getScale(),
              sourceColumn.isNullable(),
              sourceColumn.getComment());
        }
        break;
      case MAP_BY_POSITION:
        MapBiDirectional<Integer, Integer> columnMappingByPosition = transferProperties.getColumnMappingByMapPosition();
        for (Map.Entry<Integer, Integer> namedMap : columnMappingByPosition.entrySet()) {
          Integer targetPosition = namedMap.getKey();
          if (targetPosition != null) {
            DbLoggers.LOGGER_DB_ENGINE.warning("A target position (" + targetPosition + ") was given in a map by position but the target does not exist. The transfer (" + this + ") has a column mapping by position with target position but the target (" + target + ") does not exists");
            DbLoggers.LOGGER_DB_ENGINE.warning("We have ignored the target positions given");
          }
          Integer sourcePosition = namedMap.getKey();
          ColumnDef sourceColumn = source.getRelationDef().getColumnDef(sourcePosition);
          target.getOrCreateRelationDef()
            .addColumn(
              sourceColumn.getColumnName(),
              sourceColumn.getDataType().getTypeCode(),
              sourceColumn.getPrecision(),
              sourceColumn.getScale(),
              sourceColumn.isNullable(),
              sourceColumn.getComment());
        }
        break;
      case NAME:
      case POSITION:
      default:
        if (source.getOrCreateRelationDef().getColumnDefs().isEmpty()) {
          throw new RuntimeException("With the mapping column method (" + columnMappingMethod + "), we cannot create a target because the source (" + source + ") has no columns.");
        }
        target.getOrCreateRelationDef().copyDataDef(source);
        break;
    }
    Tabulars.create(target);
    transferListener.addTargetOperation(TransferResourceOperations.CREATE);
    LOGGER.info("The target data operation (" + TransferResourceOperations.CREATE + ") was executed against the target (" + target + ")");

  }


  /**
   * Check a tabular source before moving
   * * check if it exists (except for query)
   * * check if it has a structure
   */
  public void sourcePreChecks() {
    // Check source
    if (!Tabulars.exists(source)) {
      // Is it a query definition
      if (source.getScript() == null) {
        throw new RuntimeException("We cannot move the source data path (" + source + ") because it does not exist");
      }
    }

    /**
     * Column may be created at runtime
     * Example: html page over http
     * The html needs to be downloaded and parsed before the column are
     * created
     */

  }

  public TransferProperties getTransferProperties() {

    return this.transferProperties;

  }

  /**
   * You can't update, upsert or insert if you don't have the constrained columns
   */
  public void checkSourceContainsAllTargetConstrainedColumns() {

    /**
     * Source Columns
     */
    List<String> sourceColumnNames = source
      .getOrCreateRelationDef()
      .getColumnDefs()
      .stream()
      .map(ColumnDef::getColumnName)
      .collect(Collectors.toList());

    /**
     * Primary key check
     * The source should have a column name that matches the primary column name
     * of the target (otherwise the insert will be refused as this is a mandatory column)
     */
    PrimaryKeyDef targetPrimaryKey = target.getOrCreateRelationDef().getPrimaryKey();
    if (targetPrimaryKey != null) {
      for (ColumnDef targetPrimaryColumn : targetPrimaryKey.getColumns()) {
        if (!sourceColumnNames.contains(targetPrimaryColumn.getColumnName())) {
          throw new RuntimeException("The source data resource (" + source + ") does not contain a column named (" + targetPrimaryColumn.getColumnName() + "). This is mandatory because the target (" + target + ") has a primary key with this column name.");
        }
      }
    }

    /**
     * Not null column check
     */
    for (ColumnDef targetColumn : target.getOrCreateRelationDef().getColumnDefs()) {
      if (!targetColumn.isNullable()) {
        if (!sourceColumnNames.contains(targetColumn.getColumnName())) {
          throw new RuntimeException("The source data resource (" + source + ") does not contain a column named (" + targetColumn.getColumnName() + "). This is mandatory because in the target (" + target + "), this column is not null.");
        }
      }
    }
  }

  /**
   * Get the column defs in order used in an insert statement in order
   */
  public List<ColumnDef> getSourceColumnsInInsertStatement() {


    List<Integer> sourceColumnPositions = this.getTransferColumnMapping()
      .keySet()
      .stream()
      .sorted()
      .collect(Collectors.toList());

    List<ColumnDef> sourceColumnDefs = new ArrayList<>();
    for (Integer position : sourceColumnPositions) {
      sourceColumnDefs.add(source.getRelationDef().getColumnDef(position));
    }

    return sourceColumnDefs;

  }


  /**
   * @return all unique columns of the target (ie primary key and unique keys)
   */
  public List<ColumnDef> getTargetUniqueColumns() {
    List<ColumnDef> targetUniqueColumns = target.getOrCreateRelationDef().getUniqueKeys()
      .stream()
      .sorted()
      .flatMap(uk -> uk.getColumns().stream())
      .collect(Collectors.toList());
    // Add the pk columns if any
    PrimaryKeyDef primaryKey = target.getOrCreateRelationDef().getPrimaryKey();
    if (primaryKey != null) {
      targetUniqueColumns.addAll(new ArrayList<>(primaryKey.getColumns()));
    }
    return targetUniqueColumns;
  }


  public void checkThatSourceColumnsAreAlsoInTheTarget() {
    /**
     * Check that all source column names are also in the target
     */
    List<String> sourceColumnNames = source.getOrCreateRelationDef().getColumnDefs().stream()
      .map(ColumnDef::getColumnName)
      .collect(Collectors.toList());
    for (String sourceColumnName : sourceColumnNames) {
      try {
        target.getOrCreateRelationDef().getColumnDef(sourceColumnName);
      } catch (NoColumnException e) {
        TransferLog.LOGGER.warning("The column name (" + sourceColumnName + ") of the source (" + source + ") could not be found in the target table (" + target + "). It will not be used to update the target.");
      }
    }
  }

  /**
   * @return the source column names that are unique in the target
   */
  public List<ColumnDef> getSourceUniqueColumnsForTarget() {
    return getTargetUniqueColumns()
      .stream()
      .sorted()
      .filter(targetColumnName -> source.getOrCreateRelationDef().hasColumn(targetColumnName.getColumnName()))
      .collect(Collectors.toList());
  }

  public void checkBeforeUpdate() {
    checkThatSourceColumnsAreAlsoInTheTarget();
    checkThatTargetHasPrimaryOrUniqueColumns();
    checkThatSourceHasUniqueTargetColumn();
  }

  /**
   * Check that the source has a target unique column
   */
  private void checkThatSourceHasUniqueTargetColumn() {

    List<ColumnDef> targetUniqueColumns = checkThatTargetHasPrimaryOrUniqueColumns();
    List<ColumnDef> sourceUniqueColumns = this.getSourceUniqueColumnsForTarget();
    if (sourceUniqueColumns.isEmpty()) {
      throw new RuntimeException("No target unique column name was found in the source. At minimal one of the unique columns (" +
        targetUniqueColumns.stream().map(ColumnDef::getColumnName).collect(Collectors.joining(", "))
        + ") of the target (" + target + ") should appear in the source (" + source + ")");
    }
  }

  private List<ColumnDef> checkThatTargetHasPrimaryOrUniqueColumns() {
    List<ColumnDef> targetUniqueColumns = this.getTargetUniqueColumns();
    if (targetUniqueColumns.isEmpty()) {
      throw new RuntimeException("We can't create an update statement because the target table (" + target + ") has no primary key or unique columns.");
    }
    return targetUniqueColumns;
  }

  /**
   * @return the columns that should be in the set clause of an update statement
   */
  public List<ColumnDef> getSourceColumnsInUpdateSetClause() {

    this.checkBeforeUpdate();

    Set<String> sourceUniqueColumns = getSourceUniqueColumnsForTarget().stream().map(ColumnDef::getColumnName).collect(Collectors.toSet());
    List<ColumnDef> columnsInSet = source.getOrCreateRelationDef().getColumnDefs()
      .stream()
      // Without the unique columns
      .filter(col -> !sourceUniqueColumns.contains(col.getColumnName()))
      // Without the column not in the target
      .filter(col -> target.getOrCreateRelationDef().hasColumn(col.getColumnName()))
      .collect(Collectors.toList());
    if (columnsInSet.isEmpty()) {
      throw new RuntimeException("The source column names (" +
        String.join(", ", sourceUniqueColumns) + ") " +
        "are all unique columns (" +
        this.getTargetUniqueColumns().stream().map(ColumnDef::getColumnName).collect(Collectors.joining(", ")) + ") " +
        "or not present in the target. There is nothing to update, we can't then create the update query.");
    }
    return columnsInSet;
  }

  public void checkBeforeInsert() {
    checkSourceContainsAllTargetConstrainedColumns();
  }


  /**
   * @return the update alias clause (In an update clause, you need to refer to the target via an alias)
   */
  public String getTargetAlias() {
    return "target";
  }

  /**
   * @return the target column for the source column
   */
  public ColumnDef getTargetColumnFromSourceColumn(ColumnDef sourceColumnDef) throws NoColumnException {
    return target.getOrCreateRelationDef().getColumnDef(sourceColumnDef.getColumnName());
  }


  /**
   * Check before a delete
   */
  public void checkBeforeDelete() {
    checkThatTargetHasPrimaryOrUniqueColumns();
    checkThatSourceHasUniqueTargetColumn();
  }

  public List<ColumnDef> getSourceNonUniqueColumnsForTarget() {
    List<String> targetUniqueColumnNames = getTargetUniqueColumns().stream().map(ColumnDef::getColumnName).collect(Collectors.toList());
    return source
      .getOrCreateRelationDef()
      .getColumnDefs()
      .stream()
      .filter(c -> !targetUniqueColumnNames.contains(c.getColumnName()))
      .collect(Collectors.toList());
  }


}
