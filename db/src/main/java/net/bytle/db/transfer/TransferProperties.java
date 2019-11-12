package net.bytle.db.transfer;

import net.bytle.db.spi.DataPath;

public class TransferProperties {

    public static final Integer DEFAULT_COMMIT_FREQUENCY = 99999;

    // Default Fetch Size from the database
    public static final Integer DEFAULT_FETCH_SIZE = 10000;

    // Default Batch Size (Insert batch)
    public static final Integer DEFAULT_BATCH_SIZE = 10000;

    // Default target worker count
    public static final Integer DEFAULT_TARGET_WORKER_COUNT = 1;

    /**
     * The size of the {@link #setQueueSize(Integer) | queue} between the source and the target
     */
    private Integer queueSize = 2 * DEFAULT_TARGET_WORKER_COUNT * DEFAULT_FETCH_SIZE;

    /**
     * The number of threads against the target data store
     * See {@link #setTargetWorkerCount(int)}
     */
    private int targetWorkCount = DEFAULT_TARGET_WORKER_COUNT;

    /**
     * The location of the metrics data (ie snapshot of the counters by time)
     * See {@link #setMetricsPath(DataPath)}
     */
    private DataPath metricsPath;

    /**
     * The fetch size use by the source worker thread to retrieve data
     * See {@link #setFetchSize(Integer)}
     */
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
     * The load operations
     * See {@link #setLoadOperation(MoveLoadOperation)}
     */
    private MoveLoadOperation loadOperation = MoveLoadOperation.INSERT;

    /**
     * The target operation
     * See {@link #setTargetOperations(MoveTargetOperationOld...)}
     */
    private MoveTargetOperationOld[] moveTargetOperationOlds = { MoveTargetOperationOld.CREATE_IF_NOT_EXIST };

    /**
     * The source operation
     * See {@link #setSourceOperations(MoveSourceOperationOld)}
     */
    private MoveSourceOperationOld moveSourceOperationsOld;


    /**
     *
     * @return a {@link TransferProperties} instance
     */
    public static TransferProperties of() {
        return new TransferProperties();
    }

    /**
     *
     * @param queueSize - The size of the buffer queue between the source and the target
     * @return the {@link TransferProperties} instance itself for chaining instantiation
     */
    public TransferProperties setQueueSize(Integer queueSize) {
        this.queueSize = queueSize;
        return this;
    }

    /**
     *
     * @param targetWorkerCount - The number of threads against the target data store
     * @return the {@link TransferProperties} instance itself for chaining instantiation
     */
    public TransferProperties setTargetWorkerCount(int targetWorkerCount) {
        this.targetWorkCount = targetWorkerCount;
        return this;
    }

    /**
     *
     * @param feedbackFrequency - The number of rows when feedback is given back to the console
     * @return the {@link TransferProperties} instance itself for chaining instantiation
     */
    public TransferProperties setFeedbackFrequency(int feedbackFrequency) {
        this.feedbackFrequency = feedbackFrequency;
        return this;
    }

    /**
     *
     * @param metricsDataPath - The location of the metrics data (ie snapshot of the counters by time)
     * @return the {@link TransferProperties} instance itself for chaining instantiation
     */
    public TransferProperties setMetricsPath(DataPath metricsDataPath) {
        this.metricsPath = metricsDataPath;
        return this;
    }

    /**
     *
     * @param fetchSize - The fetch size use by the source worker thread to retrieve data
     * @return the {@link TransferProperties} instance itself for chaining instantiation
     *
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
     *
     * @param batchSize The batch size is the buffer unit of the target worker.
     * @return the {@link TransferProperties} instance itself for chaining instantiation
     *
     * When the number of records fetched form the {@link #setQueueSize(Integer)}  | queue} exceeds the batch size, the data is send through the network
     * to the target data store
     */
    public TransferProperties setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    /**
     *
     * @param commitFrequency
     * @return the {@link TransferProperties} instance itself for chaining instantiation
     *
     * When the number of records fetched form the {@link #setQueueSize(Integer)}  | queue} exceeds the commit frequency size, a commit is send through the network
     * to the target data store.
     *
     * A commit can be considered the same than a flush statement on a file system.
     *
     */
    public TransferProperties setCommitFrequency(Integer commitFrequency) {
        this.commitFrequency = commitFrequency;
        return this;
    }

    /**
     * Time Out in Second
     *
     *   * When the source worker have finished, this is the max delay where we will wait
     * for the termination of the target workers
     *   * When the source or target worker try to put or get data from the intermediate queue, this
     *   is the max delay
     *
     * @param timeout
     * @return the {@link TransferProperties} instance itself for chaining instantiation
     */
    public TransferProperties setTimeOut(Integer timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     *
     * Set load option:
     *   * insert (append),
     *   * update,
     *   * merge (upsert)
     * @param moveLoadOperation - an enum of {@link MoveLoadOperation}
     * @return  the {@link TransferProperties} instance itself for chaining instantiation
     */
    public TransferProperties setLoadOperation(MoveLoadOperation moveLoadOperation) {
        this.loadOperation = moveLoadOperation;
        return this;
    }

    /**
     *
     * Set operation on the target
     *   * truncate,
     *   * replace,
     *   * create
     *   ...
     * @param moveTargetOperationOlds - an enum of {@link MoveTargetOperationOld}
     * @return  the {@link TransferProperties} instance itself for chaining instantiation
     */
    public TransferProperties setTargetOperations(MoveTargetOperationOld... moveTargetOperationOlds) {
        this.moveTargetOperationOlds = moveTargetOperationOlds;
        return this;
    }

    /**
     *
     * Set operation on the source
     *   * truncate,
     *   * drop
     * @param moveSourceOperationOld - an enum of {@link MoveSourceOperationOld}
     * @return  the {@link TransferProperties} instance itself for chaining instantiation
     */
    public TransferProperties setSourceOperations(MoveSourceOperationOld moveSourceOperationOld) {
        this.moveSourceOperationsOld = moveSourceOperationOld;
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

    public Integer getQueueSize() {
        return queueSize;
    }

    public DataPath getMetricsPath() {
        return metricsPath;
    }

    /**
     *
     * @return the {@link #setTimeOut(Integer)|Timeout}
     */
    public Integer getTimeOut() {
        return timeout;
    }
}
