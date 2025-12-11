package com.tabulify.transfer;

import com.tabulify.model.ColumnDefBase;
import com.tabulify.type.MapBiDirectional;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The transfer properties that are not {@link TransferPropertiesCross cross properties}
 * <p>
 * They are passed around to:
 * * {@link TransferWorkerConsumer}
 * * {@link TransferWorkerProducer}
 * * and {@link com.tabulify.spi.DataSystem#transfer(TransferSourceTargetOrder) Data System}
 * via the {@link TransferSourceTargetOrder}
 */
public class TransferPropertiesSystem {


  /**
   * A default set of transfers
   */
  public static final TransferPropertiesSystemBuilder INSERT_WITHOUT_PARAMETERS = builder()
    .setWithParameters(false)
    .setOperation(TransferOperation.INSERT);
  public static final TransferPropertiesSystemBuilder INSERT_WITH_PARAMETERS = builder()
    .setWithParameters(true)
    .setOperation(TransferOperation.INSERT);
  public static final TransferPropertiesSystemBuilder UPSERT_WITHOUT_PARAMETERS = builder()
    .setWithParameters(false)
    .setOperation(TransferOperation.UPSERT);
  public static final TransferPropertiesSystemBuilder UPDATE_WITHOUT_PARAMETERS = builder()
    .setWithParameters(false)
    .setOperation(TransferOperation.UPDATE);
  public static final TransferPropertiesSystemBuilder DELETE_WITHOUT_PARAMETERS = builder()
    .setWithParameters(false)
    .setOperation(TransferOperation.DELETE);
  private final TransferPropertiesSystemBuilder builder;

  /**
   * Modifiable data
   */
  Set<TransferResourceOperations> sourceOperations = new HashSet<>();
  Set<TransferResourceOperations> targetOperations = new HashSet<>();
  boolean runPreDataOperation;

  private TransferPropertiesSystem(TransferPropertiesSystemBuilder builder) {
    this.builder = builder;
    sourceOperations.addAll(builder.sourceOperations);
    targetOperations.addAll(builder.targetOperations);
    runPreDataOperation = builder.runPreDataOperation;
  }

  public static TransferPropertiesSystemBuilder builder() {
    return new TransferPropertiesSystemBuilder();
  }

  /**
   * @return the TransferColumnMapping if set or null
   * Note that you should use {@link TransferSourceTargetOrder#getColumnMappingMethodOrDefault()} to get the default
   */
  public TransferMappingMethod getColumnMappingMethod() {
    return builder.columnMappingMethod;
  }

  public boolean isStrictMapping() {
    return builder.isStrictMapping;
  }

  public MapBiDirectional<Integer, Integer> getColumnMappingByMapPosition() {
    return builder.columnsPositionMap;
  }

  public MapBiDirectional<String, String> getColumnMappingByMapNamed() {
    return builder.columnsNameMap;
  }

  public TransferOperation getOperation() {
    return builder.operation;
  }

  public Set<TransferResourceOperations> getSourceOperations() {
    return this.sourceOperations;
  }

  public Set<TransferResourceOperations> getTargetOperations() {
    return this.targetOperations;
  }

  public boolean getRunPreDataOperation() {
    return this.runPreDataOperation;
  }

  /**
   * @return if this operation is a move operation
   * On the same connection, {@link TransferSourceTargetOrder#isRename()} metadata rename operation is possible
   */
  public boolean isMoveOperation() {
    if (!getSourceOperations().contains(TransferResourceOperations.DROP)) {
      return false;
    }
    if (getOperation() == TransferOperation.INSERT
      && getTargetOperations().contains(TransferResourceOperations.DROP)) {
      return true;
    }
    return getOperation() == TransferOperation.COPY;
  }

  public boolean getWithBindVariablesStatement() {
    return builder.withBindVariable;
  }

  public TransferPropertiesSystem addTargetOperations(TransferResourceOperations... transferResourceOperations) {
    this.targetOperations.addAll(Set.of(transferResourceOperations));
    return this;
  }

  public void setRunPreDataOperation(boolean runPreDataOperation) {
    this.runPreDataOperation = runPreDataOperation;
  }

  public TransferPropertiesSystem deleteTargetOperations() {
    this.targetOperations = new HashSet<>();
    return this;
  }

  public TransferPropertiesSystem deleteSourceOperations() {
    this.sourceOperations = new HashSet<>();
    return this;
  }

  public UpsertType getUpsertType() {
    return this.builder.upsertType;
  }


  public static class TransferPropertiesSystemBuilder {


    /**
     * In a mapping by name or by position, if a source column does not have a target column
     * If it's strict, the execution fails
     * If it's lose, the source column is not added to the transfer
     */
    private boolean isStrictMapping = true;

    /**
     * The source operations after the load
     */
    protected Set<TransferResourceOperations> sourceOperations = new HashSet<>();

    /**
     * The target operation before the load
     * See {@link #setTargetOperations(TransferResourceOperations...)}
     */
    private Set<TransferResourceOperations> targetOperations = new HashSet<>(Collections.singletonList(TransferResourceOperations.CREATE));

    /**
     * Sql parameter that activate the use of bind variable in statement
     */
    public boolean withBindVariable = true;

    private boolean runPreDataOperation = true;

    /**
     * The variable that holds the custom column mapping that was set by {@link #setColumnMappingByPositionalMap(Map)}
     */
    private MapBiDirectional<Integer, Integer> columnsPositionMap = new MapBiDirectional<>();

    /**
     * The column mapping by name
     */
    private MapBiDirectional<String, String> columnsNameMap = new MapBiDirectional<>();

    /**
     * How the column mapping of the transfer is done
     * <p>
     * Note that the default is at {@link TransferSourceTargetOrder#getColumnMappingMethodOrDefault()}
     */
    private TransferMappingMethod columnMappingMethod;

    /**
     * The load operations
     *
     * <p>
     * It's data store dependent ie:
     * * copy for a file system
     * * insert (create target table) for a database
     * The data system sets it (no default)
     */
    private TransferOperation operation;
    /**
     * The type of upsert
     */
    private UpsertType upsertType = UpsertType.MERGE;

    public TransferPropertiesSystem build() {

      /**
       * Check load operation
       * The load operation is important during the check of target structure
       * Why? a {@link TransferOperation#COPY} operation requires
       * the same data structure between the target and the source
       * The load is also important because it determines the order of the column
       * in the source at {@link TransferSourceTargetOrder#getSourceColumnPositionInStatementOrder}
       */
      if (operation == null) {
        /**
         * We throw because if the load operation is not set
         * in test and that we change the default, the test is no more valid,
         * and it can be confusing.
         * <p>
         * Note that Insert is the only mode that will never fail
         * In the TransferPipelineStep, the default is insert
         * for a concat (when the target is the same for all sources)
         * otherwise a copy (when the target is not the same for all sources)
         */
        throw new IllegalArgumentException("The transfer operation must be set. If you don't know, (" + TransferOperation.INSERT + ") is the recommended one by default");
      }
      return new TransferPropertiesSystem(this);
    }


    /**
     * Set operation on the target
     * * truncate,
     * * create
     * ...
     *
     * @param targetOperations - an enum of {@link TransferResourceOperations}
     * @return the {@link TransferPropertiesSystemBuilder} instance itself for chaining instantiation
     */
    public TransferPropertiesSystemBuilder setTargetOperations(TransferResourceOperations... targetOperations) {
      this.targetOperations = new HashSet<>();
      this.targetOperations.addAll(Set.of(targetOperations));
      return this;
    }

    public TransferPropertiesSystemBuilder addTargetOperations(TransferResourceOperations... targetOperations) {
      this.targetOperations.addAll(Set.of(targetOperations));
      return this;
    }

    public TransferPropertiesSystemBuilder setWithParameters(boolean withBindVariable) {

      this.withBindVariable = withBindVariable;
      return this;

    }


    /**
     * In a multiple run context, this boolean permits
     * to not execute data operations multiple time
     * <p>
     * The target pre-operation and check cannot happen multiple time
     * they need to happen once by transfer batch (ie if there is several source
     * for one target, we don't want to truncate, replace, create the target table on
     * each transfer
     */
    public TransferPropertiesSystemBuilder setRunPreDataOperation(boolean b) {
      this.runPreDataOperation = b;
      return this;
    }

    public TransferPropertiesSystemBuilder setOperation(TransferOperation transferOperation) {
      this.operation = transferOperation;
      return this;
    }

    /**
     * The column mapping will happen with the {@link ColumnDefBase#getColumnPosition() position of the column}
     *
     * @return the object for chaining
     */
    public TransferPropertiesSystemBuilder setColumnMappingByPosition() {
      this.columnMappingMethod = TransferMappingMethod.POSITION;
      return this;
    }

    /**
     * The column mapping will be done by {@link ColumnDefBase#getColumnName() column name}
     * <p>
     * By default, the column mapping is done by column position.
     * You can also give a custom column mapping relationship with the {@link #setColumnMappingByPositionalMap(Map)} function
     *
     * @return the object for chaining
     */
    public TransferPropertiesSystemBuilder setColumnMappingByName() {
      this.columnMappingMethod = TransferMappingMethod.NAME;
      return this;
    }

    /**
     * The column mapping will be customized
     *
     * @param columnsMappingByPosition - A map of the source {@link ColumnDefBase#getColumnPosition() column position} against the target {@link ColumnDefBase#getColumnPosition() column position}
     * @return the object for chaining
     */
    public TransferPropertiesSystemBuilder setColumnMappingByPositionalMap(Map<Integer, Integer> columnsMappingByPosition) {
      this.columnMappingMethod = TransferMappingMethod.MAP_BY_POSITION;
      this.columnsPositionMap = new MapBiDirectional<>(columnsMappingByPosition);
      return this;
    }


    public TransferPropertiesSystemBuilder setColumnMappingByNamedMap(Map<String, String> columnsNameMap) {
      this.columnMappingMethod = TransferMappingMethod.MAP_BY_NAME;
      this.columnsNameMap = new MapBiDirectional<>(columnsNameMap);
      return this;
    }


    public TransferOperation getOperation() {
      return this.operation;
    }

    public boolean isStrictMapping() {
      return isStrictMapping;
    }

    public TransferPropertiesSystemBuilder setStrictMapping(Boolean aBoolean) {
      isStrictMapping = aBoolean;
      return this;
    }

    public TransferPropertiesSystemBuilder setSourceOperations(TransferResourceOperations... transferDataResourceOptions) {
      this.sourceOperations = new HashSet<>();
      this.sourceOperations.addAll(Set.of(transferDataResourceOptions));
      return this;
    }

    /**
     * Add a column mapping between the source and the target
     *
     * @param sourceColumnPosition - the source position (starting at 1)
     * @param targetColumnPosition - the target position (starting at 1)
     * @return the object for chaining
     */
    public TransferPropertiesSystemBuilder setColumnMappingByPosition(int sourceColumnPosition, int targetColumnPosition) {
      this.columnMappingMethod = TransferMappingMethod.MAP_BY_POSITION;
      columnsPositionMap.put(sourceColumnPosition, targetColumnPosition);
      return this;
    }

    /**
     * Add a column mapping by name
     *
     * @param sourceColumnName - source column name
     * @param targetColumnName - target column name
     */
    public TransferPropertiesSystemBuilder setColumnMappingByName(String sourceColumnName, String targetColumnName) {
      this.columnMappingMethod = TransferMappingMethod.MAP_BY_NAME;
      this.columnsNameMap.put(sourceColumnName, targetColumnName);
      return this;
    }

    public TransferPropertiesSystemBuilder setUpsertType(UpsertType upsertType) {
      this.upsertType = upsertType;
      return this;
    }


    public boolean isReplace() {
      return this.targetOperations.contains(TransferResourceOperations.DROP);
    }

  }
}
