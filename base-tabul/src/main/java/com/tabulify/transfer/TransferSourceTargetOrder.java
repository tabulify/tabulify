package com.tabulify.transfer;

import com.tabulify.DbLoggers;
import com.tabulify.exception.DataResourceNotEmptyException;
import com.tabulify.fs.FsDataPath;
import com.tabulify.fs.textfile.FsTextDataPath;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.ColumnDefBase;
import com.tabulify.model.PrimaryKeyDef;
import com.tabulify.model.RelationDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.SchemaType;
import com.tabulify.spi.Tabulars;
import com.tabulify.exception.InternalException;
import com.tabulify.exception.NoColumnException;
import com.tabulify.type.MapBiDirectional;
import com.tabulify.type.Strings;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.tabulify.transfer.TransferMappingMethod.POSITION;

/**
 * A class that associate
 * * a source data path
 * * a target data path
 * * and all {@link TransferPropertiesCross properties}
 * <p>
 * The function has also transfer utility function.
 * <p>
 * The get functions perform also check when they expect
 * that you are building a statement (For instance {@link #getSourceColumnsInUpdateSetClause()}
 */
public class TransferSourceTargetOrder {

  public static final Logger LOGGER = Logger.getLogger(TransferSourceTargetOrder.class.getName());

  private TransferMappingMethod transferMappingMethod;
  private final DataPath target;
  private final DataPath source;
  private final TransferPropertiesSystem transferProperties;
  // column mapping by position
  private MapBiDirectional<ColumnDef<?>, ColumnDef<?>> sourceTargetColumnMap;


  /**
   * Use {@link TransferSourceTarget#buildOrder(TransferPropertiesSystem)} to build it
   */
  TransferSourceTargetOrder(DataPath sourceDataPath, DataPath targetDataPath, TransferPropertiesSystem transferProperties) {
    if (sourceDataPath != null) {
      this.source = sourceDataPath;
    } else {
      /**
       * If the source is not defined, we expect the same structure as the target
       */
      this.source = targetDataPath;
    }
    this.target = targetDataPath;
    this.transferProperties = transferProperties;
  }


  /**
   * Return the column mapping method or the default
   * Fight of the default:
   * * name: more human and logical in a sql context (a name is easier to match than an integer position)
   * * position: you don't need the target structure to match
   */
  public TransferMappingMethod getColumnMappingMethodOrDefault() {
    if (this.transferMappingMethod != null) {
      return this.transferMappingMethod;
    }

    // set explicitly
    this.transferMappingMethod = this.transferProperties.getColumnMappingMethod();
    if (transferMappingMethod != null) {
      return transferMappingMethod;
    }

    // no structure or a text file with one column
    RelationDef targetRelationDef = this.target.getRelationDef();
    if (targetRelationDef == null || this.isTargetFreeFrom()) {
      transferMappingMethod = POSITION;
      return transferMappingMethod;
    }

    // default
    transferMappingMethod = TransferMappingMethod.NAME;
    return transferMappingMethod;

  }

  /**
   * Special {@link FsTextDataPath} many to one
   * where we need to recreate all columns each time
   * Because a {@link FsTextDataPath} accepts any columns.
   * It's the only free form resource so there is no api for it
   */
  private boolean isTargetFreeFrom() {
    return Tabulars.isFreeSchemaForm(target);
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
  public MapBiDirectional<ColumnDef<?>, ColumnDef<?>> getTransferSourceTargetColumnMapping() {

    if (sourceTargetColumnMap != null) {
      return sourceTargetColumnMap;
    }

    List<? extends ColumnDef<?>> sourceColumns = source.getOrCreateRelationDef().getColumnDefs();

    TransferMappingMethod columnMappingMethod = this.getColumnMappingMethodOrDefault();
    RelationDef targetRelationDef = target.getOrCreateRelationDef();
    RelationDef sourceRelationDef = source.getRelationDef();
    switch (columnMappingMethod) {
      case POSITION:
        /**
         * one on one  (1=1, 2=2, ...)
         */
        sourceTargetColumnMap = new MapBiDirectional<>();
        for (ColumnDef sourceColumn : sourceColumns) {
          Integer sourceColumnPosition = sourceColumn.getColumnPosition();
          ColumnDef targetColumn = targetRelationDef.getColumnDef(sourceColumnPosition);
          if (targetColumn == null) {
            if (!transferProperties.isStrictMapping()) {
              continue;
            }
            String message = "Mapping by position failed. The target (" + target + ") does not have any column at the position (" + sourceColumnPosition + ") for the source column (" + sourceColumn + ")";
            message += "\nTo avoid this error, set the transfer-mapping-strict argument to false ";
            throw new RuntimeException(message);
          }
          sourceTargetColumnMap.put(sourceColumn, targetColumn);
        }
        break;
      case NAME:
        // You can't map one column to another
        sourceTargetColumnMap = new MapBiDirectional<>();
        for (ColumnDef sourceColumn : sourceColumns) {
          ColumnDef targetColumn;
          try {
            targetColumn = targetRelationDef.getColumnDef(sourceColumn.getColumnName());
            sourceTargetColumnMap.put(sourceColumn, targetColumn);
          } catch (NoColumnException e) {
            if (transferProperties.isStrictMapping()) {
              String message = "Error during the mapping of the source and target columns (by name), a column with the name (" + sourceColumn.getColumnName() + ") could not be found in the target (" + target + ")";
              message += "\nTo avoid this error, set the transfer-mapping-strict argument to false";
              throw new RuntimeException(message);
            }
          }
        }
        break;
      case MAP_BY_POSITION:
        sourceTargetColumnMap = new MapBiDirectional<>();
        MapBiDirectional<Integer, Integer> transferPropertiesSourceTargetColumnMap = transferProperties.getColumnMappingByMapPosition();
        for (Map.Entry<Integer, Integer> columnMapping : transferPropertiesSourceTargetColumnMap.entrySet()) {
          Integer sourceColumnPosition = columnMapping.getKey();
          ColumnDef sourceColumn;
          try {
            sourceColumn = sourceRelationDef.getColumnDef(sourceColumnPosition);
          } catch (Exception e) {
            throw new IllegalArgumentException("The column mapping given for the transfer (" + this + ") is not good. The source column (" + sourceColumnPosition + ") does not exists");
          }
          Integer targetColumnPosition = columnMapping.getValue();
          ColumnDef targetColumn;
          try {
            targetColumn = targetRelationDef.getColumnDef(targetColumnPosition);
          } catch (Exception e) {
            throw new IllegalArgumentException("The column mapping given for the transfer (" + this + ") is not good. The target column (" + targetColumnPosition + ") does not exists");
          }
          sourceTargetColumnMap.put(sourceColumn, targetColumn);
        }
        break;
      case MAP_BY_NAME:
        // You can't map one column to another
        sourceTargetColumnMap = new MapBiDirectional<>();
        MapBiDirectional<String, String> columnMappingsByName = transferProperties.getColumnMappingByMapNamed();
        // Check that the columns are existing and transform the map in a positional map
        for (Map.Entry<String, String> columnMapping : columnMappingsByName.entrySet()) {
          String sourceColumnName = columnMapping.getKey();
          ColumnDef sourceColumn;
          try {
            sourceColumn = sourceRelationDef.getColumnDef(sourceColumnName);
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
          sourceTargetColumnMap.put(sourceColumn, targetColumn);
        }
        break;
      default:
        throw new RuntimeException("Mapping method (" + columnMappingMethod + ") was not implemented. This is a internal bug.");
    }
    return sourceTargetColumnMap;

  }


  /**
   * @return a list of source column position that corresponds to the placeholder in the statement order
   * <p>
   * This function is used during loading (ie in SqlInsertStream) to retrieve the objects in a statement order from the source
   * The id is the {@link ColumnDefBase#getColumnPosition() column position}
   * This is SQL logic.
   */
  public List<Integer> getSourceColumnPositionInStatementOrder(SqlStatementType sqlStatementType) {

    switch (sqlStatementType) {
      case UPDATE: {
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

      }
      case DELETE: {

        /**
         * The unique columns in the where clause
         */
        List<Integer> sourceColumnPositionInStatementOrder = new ArrayList<>();
        getSourceUniqueColumnsForTarget().forEach(c -> sourceColumnPositionInStatementOrder.add(c.getColumnPosition()));
        return sourceColumnPositionInStatementOrder;

      }
      case INSERT:
      case MERGE: {

        return getSourceColumnsInInsertStatement()
          .stream()
          .map(ColumnDef::getColumnPosition)
          .collect(Collectors.toList());

      }
      default:
        MapBiDirectional<ColumnDef<?>, ColumnDef<?>> columnMapping = getTransferSourceTargetColumnMapping();
        List<Integer> sourceColumnPosition = new ArrayList<>();
        for (int i = 1; i <= target.getRelationDef().getColumnsSize(); i++) {
          ColumnDef<?> targetColumn = target.getRelationDef().getColumnDef(i);
          ColumnDef<?> sourceColumn = columnMapping.getKey(targetColumn);
          if (sourceColumn == null) {
            // target column not in the transfer
            continue;
          }
          sourceColumnPosition.add(sourceColumn.getColumnPosition());
        }
        return sourceColumnPosition;
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
   * once by target and pass different {@link TransferPropertiesCross}
   * with different {@link TransferResourceOperations}
   * ie you don't want to truncate or delete each time
   *
   * @param transferListener -
   * @param createTarget     - a switch to say if the function should create the table
   *                         (it was introduced to be able to support the `create table as` sql statement.
   *                         If this parameter is false, no creation and table check is performed if the target does not exist
   */
  public void targetPreOperationsAndCheck(TransferListener transferListener, boolean createTarget) {


    boolean isFreeFormTarget = this.isTargetFreeFrom();

    boolean runPreDataOperation = this.getTransferProperties().getRunPreDataOperation();
    if (runPreDataOperation) {

      /**
       * Replace Pre-operations
       * Create is done in the next step
       */
      if (transferProperties.getTargetOperations().contains(TransferResourceOperations.DROP)) {
        if (Tabulars.exists(target)) {
          Tabulars.drop(target);
          LOGGER.info("The target data operation (" + TransferResourceOperations.DROP + ") was executed against the target (" + target + ") because of the (" + TransferResourceOperations.DROP + ") target operation");
          transferListener.addTargetOperation(TransferResourceOperations.DROP);
        }

      }


      /**
       * In case the data system create itself the target
       * We just stop here
       * (`create table as` statement in sql for instance)
       */
      if (!createTarget) {
        return;
      }

      /**
       * A rename does not need to create the target
       * A move don't create any target
       */
      if (this.isRename()) {
        return;
      }

      /**
       * Target structure
       */
      boolean createTargetStructure = this.getCreateTargetStructureFirstRun(isFreeFormTarget);
      if (createTargetStructure) {
        this.createTargetStructure();
      }

      /**
       * Creation
       */
      final Boolean targetExists = Tabulars.exists(target);
      if (!targetExists) {
        if (
          (transferProperties.getTargetOperations().contains(TransferResourceOperations.CREATE)
            || transferProperties.getTargetOperations().contains(TransferResourceOperations.DROP))
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

      /**
       * Empty check
       * (Target should be empty for a copy, move operation if there is no truncate, drop)
       */
      if (
        transferProperties.getOperation() == TransferOperation.COPY
          &&
          !(
            transferProperties.getTargetOperations().contains(TransferResourceOperations.TRUNCATE)
              || transferProperties.getTargetOperations().contains(TransferResourceOperations.DROP)
          )
      ) {
        if (!Tabulars.isEmpty(target)) {
          throw new DataResourceNotEmptyException("The target data (" + target + ") is not empty. In a copy operation, the target resource should be empty or one of the following target operation (" + TransferResourceOperations.TRUNCATE + ", " + TransferResourceOperations.DROP + ") should be present");
        }
      }

    } else {

      /**
       * We are here after the first run,
       * where initialization operations have been already done
       * but in a free form target, we need to recreate each time the structure
       * so that the {@link #getTransferSourceTargetColumnMapping()}  column mapping} by position don't fail
       */
      if (isFreeFormTarget) {
        this.createTargetStructure();
      }

    }


    /**
     * Data structure checks
     * The structure should have been created before
     */
    if (target.getOrCreateRelationDef().getColumnsSize() == 0) {
      throw new InternalException("Internal Error: The target (" + target + ") does not have any columns.");
    }

    /**
     * Move and copy should have the same data structure (same number of columns)
     */
    TransferOperation transferOperation = transferProperties.getOperation();
    if (transferOperation == null) {
      throw new RuntimeException("Internal error, the default load operation is null. It should have already been set by the program.");
    }
    if (transferOperation.requireSameStructureBetweenSourceAndTarget()) {
      TransferMappingMethod columnMappingMethod = this.getColumnMappingMethodOrDefault();
      switch (columnMappingMethod) {
        case NAME:
        case POSITION:
          break;
        default:
          throw new RuntimeException("You can not use the column mapping method (" + columnMappingMethod + ") because the transfer operation (" + transferOperation + ") requires to have the same columns between the source and the target.\n" +
            "You can:\n" +
            "  * use the column mapping (" + TransferMappingMethod.NAME + ") or (" + POSITION + ")\n" +
            "  * or change the transfer operation to (" + TransferOperation.UPSERT + ") or (" + TransferOperation.INSERT + ")");
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
              throw new InternalException("The load operation (" + transferOperation + ") should have a branch in this switch");
          }
        } catch (NoColumnException e) {
          String message = "Unable to " + transferOperation + " the data unit (" + source + ") because it exists already in the target location (" + target + ") with a different structure" +
            " (The source column (" + columnDef.getColumnName() + "," + columnDef.getColumnPosition() + ") was not found in the target data unit)";
          DbLoggers.LOGGER_DB_ENGINE.severe(message);
          throw new RuntimeException(message);
        }
      }
    }

    /**
     * Column Mapping check
     * This function will check that the {@link #getTransferSourceTargetColumnMapping() column mapping}
     * More particularly that the target data type must be able to receive the source data
     * Log on an info level because the cast mechanism can transform it before loading.
     * See SqlDataStore#toSqlObject that uses {@link com.tabulify.type.Casts#cast(Object, Class)}
     */
    Map<ColumnDef<?>, ColumnDef<?>> columnMapping = getTransferSourceTargetColumnMapping();
    columnMapping.entrySet().forEach(c -> {
      ColumnDef<?> sourceColumn = c.getKey();
      ColumnDef<?> targetColumn = c.getValue();
      if (sourceColumn.getDataType().getVendorTypeNumber() != targetColumn.getDataType().getVendorTypeNumber()) {
        String message = Strings.createMultiLineFromStrings(
          "There is a potential problem with a data loading mapping between two columns",
          "The problem is on the mapping (" + c + ") between the source column (" + sourceColumn + ") and the target column (" + targetColumn + ")",
          "where the source data type (" + sourceColumn.getDataType().toKeyNormalizer() + ") is different than the target data type (" + targetColumn.getDataType().toKeyNormalizer() + ")."
        ).toString();
        LOGGER.fine(message);
      }
    });


  }

  /**
   * Same system with a move,
   * it's a {@link TransferMethod#RENAME}
   */
  public boolean isRename() {
    return this.transferProperties.isMoveOperation()
      && target.getConnection().getServiceId().equals(source.getConnection().getServiceId())
      && !source.isRuntime();
  }

  void createTarget(TransferListener transferListener) {
    target.getConnection().getDataSystem().create(target, source, null);
    transferListener.addTargetOperation(TransferResourceOperations.CREATE);
    LOGGER.info("The target data operation (" + TransferResourceOperations.CREATE + ") was executed against the target (" + target + ")");
  }

  /**
   * Target structure creation
   * We have the params to be able to create the graph of processing
   */
  void createTargetStructure() {


    RelationDef targetRelationDef;
    if (transferProperties.getTargetOperations().contains(TransferResourceOperations.DROP)) {
      targetRelationDef = target.createEmptyRelationDef();
    } else {
      targetRelationDef = target.getOrCreateRelationDef();
    }

    /**
     * example
     * File: If this for instance, the creation of a file, the file may not exist
     * and have no content and therefore no structure
     */
    TransferMappingMethod columnMappingMethod = this.getColumnMappingMethodOrDefault();
    switch (columnMappingMethod) {
      case MAP_BY_NAME:
        MapBiDirectional<String, String> namedMaps = transferProperties.getColumnMappingByMapNamed();
        for (Map.Entry<String, String> namedMap : namedMaps.entrySet()) {
          ColumnDef<?> sourceColumn;
          String key = namedMap.getKey();
          try {
            sourceColumn = source.getRelationDef().getColumnDef(key);
          } catch (NoColumnException e) {
            throw new IllegalArgumentException("The column at the position (" + key + ") for the source resource (" + source + ") does not exists");
          }
          String targetColumnName = namedMap.getValue();
          targetRelationDef
            .addColumn(
              targetColumnName,
              targetRelationDef.getDataPath().getConnection().getSqlDataTypeFromSourceColumn(sourceColumn),
              sourceColumn.getPrecision(),
              sourceColumn.getScale(),
              sourceColumn.isNullable(),
              sourceColumn.getComment());
        }
        break;
      case MAP_BY_POSITION:
        MapBiDirectional<Integer, Integer> columnMappingByMapPosition = transferProperties.getColumnMappingByMapPosition();
        List<Integer> targetColumnPositions = columnMappingByMapPosition
          .values()
          .stream()
          .sorted()
          .collect(Collectors.toList());
        for (Integer targetColumnPosition : targetColumnPositions) {
          Integer sourcePosition = columnMappingByMapPosition.getKey(targetColumnPosition);
          ColumnDef<?> sourceColumn = source.getRelationDef().getColumnDef(sourcePosition);
          if (sourceColumn == null) {
            throw new IllegalArgumentException("The transfer (" + this + ") has a column mapping by position with a source position (" + sourcePosition + ") but the source (" + source + ") have only " + source.getRelationDef().getColumnsSize() + " columns.");
          }
          targetRelationDef.addColumn(
            sourceColumn.getColumnName(),
            targetRelationDef.getDataPath().getConnection().getSqlDataTypeFromSourceColumn(sourceColumn),
            sourceColumn.getPrecision(),
            sourceColumn.getScale(),
            sourceColumn.isNullable(),
            sourceColumn.getComment()
          );
        }
        break;
      case NAME:
      case POSITION:
      default:
        if (source.getOrCreateRelationDef().getColumnDefs().isEmpty()) {
          throw new RuntimeException("With the mapping column method (" + columnMappingMethod + "), we cannot create a target because the source (" + source + ", media type" + source.getMediaType() + ") has no columns.");
        }
        targetRelationDef.copyDataDef(source);
        break;
    }


  }

  /**
   * Should we copy/create the structure
   * in the first run
   */
  private boolean getCreateTargetStructureFirstRun(Boolean isFreeFormTarget) {


    RelationDef targetRelationDef = target.getOrCreateRelationDef();
    if (isFreeFormTarget) {
      targetRelationDef.dropAll();
      return true;
    }

    if (!Tabulars.exists(target)) {
      return true;
    }

    boolean emptyFile = target instanceof FsDataPath && target.getSize() == 0;
    if (emptyFile) {
      // we drop it so that if there is a structure, it's created
      Tabulars.drop(target);
      return true;
    }

    return false;
  }


  /**
   * Check a tabular source before moving
   * * check if it exists (except for query)
   * * check if it has a structure
   */
  public void sourcePreChecks() {

    if (source.isRuntime()) {
      // Is it an executable definition
      // Executable does not exist by definition
      DataPath executableDataPath = source.getExecutableDataPath();
      if (executableDataPath != null && !Tabulars.exists(executableDataPath)) {
        throw new IllegalArgumentException("We cannot transfer the executable (" + executableDataPath + ") because the executable resource does not exist");
      }
    } else {
      // Check source
      if (!Tabulars.exists(source)) {
        throw new IllegalArgumentException("We cannot transfer the source data path (" + source + ") because it does not exist");
      }
    }

    /**
     * Column may be created at runtime
     * Example: html page over http
     * The html needs to be downloaded and parsed before the column are
     * created
     */

  }

  public TransferPropertiesSystem getTransferProperties() {

    return this.transferProperties;

  }

  /**
   * You can't update, upsert or insert if you don't have the constrained columns
   */
  public void checkSourceContainsAllTargetConstrainedColumns() {

    /**
     * Source Columns
     */
    MapBiDirectional<ColumnDef<?>, ColumnDef<?>> localSourceTargetColumnMap = getTransferSourceTargetColumnMapping();


    /**
     * Primary key check
     * The source should have a column name that matches the primary column name
     * of the target (otherwise the insert will be refused as this is a mandatory column)
     */
    PrimaryKeyDef targetPrimaryKey = target.getOrCreateRelationDef().getPrimaryKey();
    if (targetPrimaryKey != null) {
      for (ColumnDef targetPrimaryColumn : targetPrimaryKey.getColumns()) {
        ColumnDef sourceColumn = localSourceTargetColumnMap.getKey(targetPrimaryColumn);
        if (sourceColumn == null) {
          throw new RuntimeException("The source data resource (" + source + ") does not contain a column named (" + targetPrimaryColumn.getColumnName() + "). This is mandatory because the target (" + target + ") has a primary key with this column name.");
        }
      }
    }

    /**
     * Not null column check
     *
     */
    if (source.getSchemaType() == SchemaType.STRICT) {
      for (Map.Entry<ColumnDef<?>, ColumnDef<?>> sourceTargetColumn : localSourceTargetColumnMap.entrySet()) {
        ColumnDef<?> targetColumn = sourceTargetColumn.getValue();
        if (!targetColumn.isNullable()) {
          ColumnDef<?> sourceColumn = sourceTargetColumn.getKey();
          if (sourceColumn.isNullable()) {
            throw new RuntimeException("The target column " + targetColumn + " is not nullable but it's source is (" + sourceColumn + ").");
          }
        }
      }
    }
  }

  /**
   * Return the source column defs in order used in an insert statement
   * <p>
   * The source column in an insert statement have the order of the source
   * Why? because we can't change it for now when the source is a select statement
   * <p>
   * The counterpart is {@link #getTargetColumnInInsertStatement()}
   */
  public List<ColumnDef<?>> getSourceColumnsInInsertStatement() {

    return this.getTransferSourceTargetColumnMapping()
      .keySet()
      .stream()
      .sorted(Comparator.comparing(ColumnDef::getColumnPosition))
      .collect(Collectors.toList());


  }


  /**
   * @return all unique columns of the target (ie primary key and unique keys)
   */
  public List<ColumnDef<?>> getTargetUniqueColumns() {
    List<ColumnDef<?>> targetUniqueColumns = target.getOrCreateRelationDef().getUniqueKeys()
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
        TransferLog.LOGGER.warning(()->"The column name (" + sourceColumnName + ") of the source (" + source + ") could not be found in the target table (" + target + "). It will not be used to update the target.");
      }
    }
  }

  /**
   * @return the source column that are unique in the target
   */
  public List<ColumnDef<?>> getSourceUniqueColumnsForTarget() {
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

    List<ColumnDef<?>> targetUniqueColumns = checkThatTargetHasPrimaryOrUniqueColumns();
    List<ColumnDef<?>> sourceUniqueColumns = this.getSourceUniqueColumnsForTarget();
    if (sourceUniqueColumns.isEmpty()) {
      throw new RuntimeException("No target unique column name was found in the source. At minimal one of the unique columns (" +
        targetUniqueColumns.stream().map(ColumnDef::getColumnName).collect(Collectors.joining(", "))
        + ") of the target (" + target + ") should appear in the source (" + source + ")");
    }
  }

  private List<ColumnDef<?>> checkThatTargetHasPrimaryOrUniqueColumns() {
    List<ColumnDef<?>> targetUniqueColumns = this.getTargetUniqueColumns();
    if (targetUniqueColumns.isEmpty()) {
      throw new RuntimeException("We can't create an update statement because the target table (" + target + ") has no primary key or unique columns.");
    }
    return targetUniqueColumns;
  }

  /**
   * @return the columns that should be in the set clause of an update statement
   */
  public List<ColumnDef<?>> getSourceColumnsInUpdateSetClause() {

    this.checkBeforeUpdate();

    Set<String> sourceUniqueColumns = getSourceUniqueColumnsForTarget().stream().map(ColumnDef::getColumnName).collect(Collectors.toSet());
    List<ColumnDef<?>> columnsInSet = source.getOrCreateRelationDef().getColumnDefs()
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
    return getTransferSourceTargetColumnMapping().get(sourceColumnDef);
  }


  /**
   * Check before a `delete`
   */
  public void checkBeforeDelete() {
    checkThatTargetHasPrimaryOrUniqueColumns();
    checkThatSourceHasUniqueTargetColumn();
  }

  public List<ColumnDef<?>> getSourceNonUniqueColumnsForTarget() {
    List<String> targetUniqueColumnNames = getTargetUniqueColumns().stream().map(ColumnDef::getColumnName).collect(Collectors.toList());
    return source
      .getOrCreateRelationDef()
      .getColumnDefs()
      .stream()
      .filter(c -> !targetUniqueColumnNames.contains(c.getColumnName()))
      .collect(Collectors.toList());
  }


  /**
   * Target column in insert statement follow the source order
   * why? if the source is a select statement, we can't change the column in a select statement
   */
  public List<? extends ColumnDef<?>> getTargetColumnInInsertStatement() {
    return this.getTransferSourceTargetColumnMapping()
      .keySet()
      .stream()
      .sorted(Comparator.comparing(ColumnDef::getColumnPosition))
      .map(e -> this.getTransferSourceTargetColumnMapping().get(e))
      .collect(Collectors.toList());
  }

}
