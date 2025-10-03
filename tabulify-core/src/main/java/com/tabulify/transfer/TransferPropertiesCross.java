package com.tabulify.transfer;

import com.tabulify.spi.DataPath;

/**
 * Transfer Cross Properties that are used in a cross transfer
 * The other transfer properties that are passed around to system are at {@link TransferPropertiesSystem}
 */
public class TransferPropertiesCross {


  public static final Integer DEFAULT_COMMIT_FREQUENCY = Integer.MAX_VALUE;

  // Default Fetch Size from the database
  public static final Integer DEFAULT_FETCH_SIZE = 10000;

  // Default Batch Size (Insert batch)
  public static final Integer DEFAULT_BATCH_SIZE = 10000;

  // Default target worker count
  public static final Integer DEFAULT_TARGET_WORKER_COUNT = 1;


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
   * @return a {@link TransferPropertiesCross} instance
   */
  public static TransferPropertiesCross create() {
    return new TransferPropertiesCross();
  }

  /**
   * @param queueSize - The size of the buffer queue between the source and the target
   * @return the {@link TransferPropertiesCross} instance itself for chaining instantiation
   */
  public TransferPropertiesCross setBufferSize(Integer queueSize) {
    this.bufferSize = queueSize;
    return this;
  }

  /**
   * @param targetWorkerCount - The number of threads against the target data store
   * @return the {@link TransferPropertiesCross} instance itself for chaining instantiation
   */
  public TransferPropertiesCross setTargetWorkerCount(int targetWorkerCount) {
    this.targetWorkCount = targetWorkerCount;
    return this;
  }

  /**
   * @param feedbackFrequency - The number of rows when a feedback is given back to the console
   * @return the {@link TransferPropertiesCross} instance itself for chaining instantiation
   */
  public TransferPropertiesCross setFeedbackFrequency(int feedbackFrequency) {
    this.feedbackFrequency = feedbackFrequency;
    return this;
  }

  /**
   * @param metricsDataPath - The location of the metrics data (ie snapshot of the counters by time)
   * @return the {@link TransferPropertiesCross} instance itself for chaining instantiation
   */
  public TransferPropertiesCross setMetricsDataPath(DataPath metricsDataPath) {
    this.metricsPath = metricsDataPath;
    return this;
  }

  /**
   * @param fetchSize - The fetch size use by the source worker thread to retrieve data
   * @return the {@link TransferPropertiesCross} instance itself for chaining instantiation
   * <p>
   * Number of rows fetched with each data source round trip for a query,
   * 10 for Oracle row-prefetch value.
   * http://docs.oracle.com/cd/B19306_01/java.102/b14355/resltset.htm#i1023619
   * Changes made to the fetch size of a statement object after a result set is produced will have
   * no affect on that result set.
   */
  public TransferPropertiesCross setFetchSize(Integer fetchSize) {
    this.fetchSize = fetchSize;
    return this;
  }

  /**
   * @param batchSize The number of rows that defined the buffer size
   * @return the {@link TransferPropertiesCross} instance itself for chaining instantiation
   * <p>
   * When the number of records fetched form the {@link #setBufferSize(Integer)}  | queue} exceeds the batch size, the data is send through the network
   * to the target data store
   * <p>
   * The same notion that batches in JDBC.
   */
  public TransferPropertiesCross setBatchSize(Integer batchSize) {
    this.batchSize = batchSize;
    return this;
  }


  /**
   * @param commitFrequency - The commit frequency based in batch unit
   * @return the {@link TransferPropertiesCross} instance itself for chaining instantiation
   * <p>
   * A commit frequency of 3 means that there is a commit (flush) every 3 batches (
   * <p>
   * <p>
   * When the number of records fetched form the {@link #setBufferSize(Integer)}  | queue} exceeds the commit frequency size, a commit is send through the network
   * to the target data store.
   * <p>
   * A commit can be considered the same as a flush statement on a file system.
   */
  public TransferPropertiesCross setCommitFrequency(Integer commitFrequency) {
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
   * @return the {@link TransferPropertiesCross} instance itself for chaining instantiation
   */
  public TransferPropertiesCross setTimeOut(Integer timeoutSecond) {
    this.timeout = timeoutSecond;
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


}
