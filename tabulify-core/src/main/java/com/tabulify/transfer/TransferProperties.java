package com.tabulify.transfer;

import com.tabulify.model.ColumnDefBase;
import com.tabulify.spi.DataPath;
import net.bytle.type.MapBiDirectional;

import java.util.*;

/**
 * A transfer process may transfer multiple {@link TransferSourceTarget transfer source target} at once
 * The properties below are process properties
 * ie properties that are not related to the source and/of target data path
 * <p>
 * We can pass them around to consumer and producer
 * <p>
 */
public class TransferProperties {


  /**
   * In a multiple run context, this boolean permits
   * to not execute data operations multiple time
   * <p>
   * The target pre-operation and check cannot happen multiple time
   * they need to happen once by transfer batch (ie if there is several source
   * for one target, we don't want to truncate, replace, create the target table each time)
   */
  private boolean runPreDataOperation = true;


  public static final Integer DEFAULT_COMMIT_FREQUENCY = Integer.MAX_VALUE;

  // Default Fetch Size from the database
  public static final Integer DEFAULT_FETCH_SIZE = 10000;

  // Default Batch Size (Insert batch)
  public static final Integer DEFAULT_BATCH_SIZE = 10000;

  // Default target worker count
  public static final Integer DEFAULT_TARGET_WORKER_COUNT = 1;

  /**
   * A default set of transfers
   */
  public static final TransferProperties INSERT_WITHOUT_BIND_VARIABLE = TransferProperties.create().setWithBindVariablesStatement(false).setOperation(TransferOperation.INSERT);
  public static final TransferProperties INSERT_WITH_BIND_VARIABLE = TransferProperties.create().setWithBindVariablesStatement(true).setOperation(TransferOperation.INSERT);
  public static final TransferProperties UPSERT_WITHOUT_BIND_VARIABLE = TransferProperties.create().setWithBindVariablesStatement(false).setOperation(TransferOperation.UPSERT);
  public static final TransferProperties UPDATE_WITHOUT_BIND_VARIABLE = TransferProperties.create().setWithBindVariablesStatement(false).setOperation(TransferOperation.UPDATE);
  public static final TransferProperties DELETE_WITHOUT_BIND_VARIABLE = TransferProperties.create().setWithBindVariablesStatement(false).setOperation(TransferOperation.DELETE);


  /**
   * The size of the {@link #setBufferSize(Integer) | queue} between the source and the target
   */
  private Integer bufferSize = 2 * DEFAULT_TARGET_WORKER_COUNT * DEFAULT_FETCH_SIZE;

  /**
   * The number of threads against the target data store
   * See {@link #setTargetWorkerCount(int)}
   */
  private int targetWorkCount = DEFAULT_TARGET_WORKER_COUNT;

  /**
   * How the column mapping of the transfer is done
   * <p>
   * This is by position because you don't need
   * the target structure to match.
   */
  private TransferColumnMapping columnMappingMethod = TransferColumnMapping.POSITION;


  /**
   * The location of the metrics data (ie snapshot of the counters by time)
   * See {@link #setMetricsDataPath(DataPath)}
   */
  private DataPath metricsPath;

  /**
   * The fetch size use by the source worker thread to retrieve data
   * See {@link #setFetchSize(Integer)}
   */
  @SuppressWarnings("FieldCanBeLocal")
  private Integer fetchSize;

  /**
   * The batch size is the buffer unit of the target worker.
   * See {@link #setBatchSize(Integer)}
   */
  private Integer batchSize;

  /**
   * The commit frequency of the target worker count
   * See {@link #setCommitFrequency(Integer)}
   */
  private Integer commitFrequency;

  /**
   * See {@link #setTimeOut(Integer)}
   */
  private Integer timeout = Integer.MAX_VALUE;

  /**
   * See {@link #setFeedbackFrequency(int)}
   */
  private Integer feedbackFrequency;


  /**
   * The target operation before the load
   * See {@link #addTargetOperations(TransferResourceOperations...)}
   */
  private final Set<TransferResourceOperations> transferTargetOperations = new HashSet<>(Collections.singletonList(TransferResourceOperations.CREATE));

  /**
   * The source operations after the load
   */
  protected Set<TransferResourceOperations> transferSourceOperations = new HashSet<>();

  /**
   * The load operations
   *
   * <p>
   * It's data store dependent ie:
   * * copy for a file system
   * * insert (create target table) for a database
   * The data system sets it (no default)
   */
  private TransferOperation transferOperation;


  /**
   * The variable that holds the custom column mapping that was set by {@link #withColumnMappingByPositionalMap(Map)}
   */
  private MapBiDirectional<Integer, Integer> columnMappingByMapPosition = new MapBiDirectional<>();
  /**
   * Sql parameter that activate the use of bind variable in statement
   */
  private Boolean withBindVariablesStatement = true;
  /**
   * The column mapping by name
   */
  private MapBiDirectional<String, String> columnMappingByMapName = new MapBiDirectional<>();


  /**
   * @return a {@link TransferProperties} instance
   */
  public static TransferProperties create() {
    return new TransferProperties();
  }

  /**
   * @param queueSize - The size of the buffer queue between the source and the target
   * @return the {@link TransferProperties} instance itself for chaining instantiation
   */
  public TransferProperties setBufferSize(Integer queueSize) {
    this.bufferSize = queueSize;
    return this;
  }

  /**
   * @param targetWorkerCount - The number of threads against the target data store
   * @return the {@link TransferProperties} instance itself for chaining instantiation
   */
  public TransferProperties setTargetWorkerCount(int targetWorkerCount) {
    this.targetWorkCount = targetWorkerCount;
    return this;
  }

  /**
   * @param feedbackFrequency - The number of rows when a feedback is given back to the console
   * @return the {@link TransferProperties} instance itself for chaining instantiation
   */
  public TransferProperties setFeedbackFrequency(int feedbackFrequency) {
    this.feedbackFrequency = feedbackFrequency;
    return this;
  }

  /**
   * @param metricsDataPath - The location of the metrics data (ie snapshot of the counters by time)
   * @return the {@link TransferProperties} instance itself for chaining instantiation
   */
  public TransferProperties setMetricsDataPath(DataPath metricsDataPath) {
    this.metricsPath = metricsDataPath;
    return this;
  }

  /**
   * @param fetchSize - The fetch size use by the source worker thread to retrieve data
   * @return the {@link TransferProperties} instance itself for chaining instantiation
   * <p>
   * Number of rows fetched with each data source round trip for a query,
   * 10 for Oracle row-prefetch value.
   * http://docs.oracle.com/cd/B19306_01/java.102/b14355/resltset.htm#i1023619
   * Changes made to the fetch size of a statement object after a result set is produced will have
   * no affect on that result set.
   */
  public TransferProperties setFetchSize(Integer fetchSize) {
    this.fetchSize = fetchSize;
    return this;
  }

  /**
   * @param batchSize The number of rows that defined the buffer size
   * @return the {@link TransferProperties} instance itself for chaining instantiation
   * <p>
   * When the number of records fetched form the {@link #setBufferSize(Integer)}  | queue} exceeds the batch size, the data is send through the network
   * to the target data store
   * <p>
   * The same notion that batches in JDBC.
   */
  public TransferProperties setBatchSize(Integer batchSize) {
    this.batchSize = batchSize;
    return this;
  }

  public TransferProperties setWithBindVariablesStatement(Boolean withBindVariablesStatement) {
    assert withBindVariablesStatement != null;
    this.withBindVariablesStatement = withBindVariablesStatement;
    return this;
  }

  /**
   * @param commitFrequency - The commit frequency based in batch unit
   * @return the {@link TransferProperties} instance itself for chaining instantiation
   * <p>
   * A commit frequency of 3 means that there is a commit (flush) every 3 batches (
   * <p>
   * <p>
   * When the number of records fetched form the {@link #setBufferSize(Integer)}  | queue} exceeds the commit frequency size, a commit is send through the network
   * to the target data store.
   * <p>
   * A commit can be considered the same than a flush statement on a file system.
   */
  public TransferProperties setCommitFrequency(Integer commitFrequency) {
    this.commitFrequency = commitFrequency;
    return this;
  }


  /**
   * Time Out in Second
   * <p>
   * * When the source worker have finished, this is the max delay where we will wait
   * for the termination of the target workers
   * * When the source or target worker try to put or get data from the intermediate queue, this
   * is the max delay
   *
   * @param timeoutSecond the timeout in second
   * @return the {@link TransferProperties} instance itself for chaining instantiation
   */
  public TransferProperties setTimeOut(Integer timeoutSecond) {
    this.timeout = timeoutSecond;
    return this;
  }


  /**
   * Set operation on the target
   * * truncate,
   * * replace,
   * * create
   * ...
   *
   * @param transferDataResourceOptions - an enum of {@link TransferResourceOperations}
   * @return the {@link TransferProperties} instance itself for chaining instantiation
   */
  public TransferProperties addTargetOperations(TransferResourceOperations... transferDataResourceOptions) {
    this.transferTargetOperations.addAll(Arrays.asList(transferDataResourceOptions));
    return this;
  }


  /**
   * @return the {@link #setTargetWorkerCount(int) | the target worker count}
   */
  public int getTargetWorkerCount() {
    return targetWorkCount;
  }

  /**
   * @return the {@link #setBatchSize(Integer) | batchSize}
   */
  public Integer getBatchSize() {
    return batchSize;
  }

  public Integer getCommitFrequency() {
    return commitFrequency;
  }


  public Integer getFeedbackFrequency() {
    return feedbackFrequency;
  }

  public Integer getBufferSize() {
    return bufferSize;
  }

  public DataPath getMetricsPath() {
    return metricsPath;
  }

  /**
   * @return the {@link #setTimeOut(Integer)|Timeout}
   */
  public Integer getTimeOut() {
    return timeout;
  }


  @SuppressWarnings("UnusedReturnValue")
  public TransferProperties addSourceOperations(TransferResourceOperations... transferDataResourceOptions) {
    this.transferSourceOperations.addAll(Arrays.asList(transferDataResourceOptions));
    return this;
  }

  public TransferOperation getOperation() {
    return this.transferOperation;
  }

  public Set<TransferResourceOperations> getTargetOperations() {
    return this.transferTargetOperations;
  }

  public TransferProperties setOperation(TransferOperation transferOperation) {
    this.transferOperation = transferOperation;
    return this;
  }

  /**
   * The column mapping will be customized
   *
   * @param columnMapping - A map of the source {@link ColumnDefBase#getColumnPosition() column position} against the target {@link ColumnDefBase#getColumnPosition() column position}
   * @return the object for chaining
   */
  public TransferProperties withColumnMappingByPositionalMap(Map<Integer, Integer> columnMapping) {
    this.columnMappingMethod = TransferColumnMapping.MAP_BY_POSITION;
    // Reset the data to empty map
    this.columnMappingByMapPosition = new MapBiDirectional<>();
    // Add each column - columnMapping function is the driver (it performs the test, set the method,...)
    columnMapping.forEach(this::addColumnMappingByPosition);
    return this;
  }

  /**
   * The column mapping will happen with the {@link ColumnDefBase#getColumnPosition() position of the column}
   *
   * @return the object for chaining
   */
  public TransferProperties withColumnMappingByPosition() {
    this.columnMappingMethod = TransferColumnMapping.POSITION;
    return this;
  }

  public TransferColumnMapping getColumnMappingMethod() {
    return this.columnMappingMethod;
  }

  /**
   * Add a column mapping between the source and the target
   *
   * @param sourceColumnPosition - the source position (starting at 1)
   * @param targetColumnPosition - the target position (starting at 1)
   * @return the object for chaining
   */
  public TransferProperties addColumnMappingByPosition(int sourceColumnPosition, int targetColumnPosition) {
    this.columnMappingMethod = TransferColumnMapping.MAP_BY_POSITION;
    columnMappingByMapPosition.put(sourceColumnPosition, targetColumnPosition);
    return this;
  }


  /**
   * The column mapping will be done by {@link ColumnDefBase#getColumnName() column name}
   * <p>
   * By default, the column mapping is done by column position.
   * You can also give a custom column mapping relationship with the {@link #withColumnMappingByPositionalMap(Map)} function
   *
   * @return the object for chaining
   */
  public TransferProperties withColumnMappingByName() {
    this.columnMappingMethod = TransferColumnMapping.NAME;
    return this;
  }


  public MapBiDirectional<Integer, Integer> getColumnMappingByMapPosition() {
    return this.columnMappingByMapPosition;
  }

  public boolean withBindVariablesStatement() {
    return this.withBindVariablesStatement;
  }

  public Set<TransferResourceOperations> getSourceOperations() {
    return this.transferSourceOperations;
  }


  public TransferProperties withColumnMappingByNamedMap(Map<String, String> mappingByName) {
    this.columnMappingMethod = TransferColumnMapping.MAP_BY_NAME;
    // Reset the data to empty map
    this.columnMappingByMapName = new MapBiDirectional<>();
    // Add each column - columnMapping function is the driver (it performs the test, set the method,...)
    mappingByName.forEach(this::addColumnMappingByName);
    return this;
  }

  private TransferProperties addColumnMappingByName(String sourceColumnName, String targetColumnName) {
    this.columnMappingMethod = TransferColumnMapping.MAP_BY_NAME;
    this.columnMappingByMapName.put(sourceColumnName, targetColumnName);
    return this;
  }

  public MapBiDirectional<String, String> getColumnMappingByMapNamed() {
    return this.columnMappingByMapName;
  }

  public void setRunPreDataOperation(boolean b) {
    this.runPreDataOperation = b;
  }

  public boolean getRunPreDataOperation() {
    return this.runPreDataOperation;
  }

}
