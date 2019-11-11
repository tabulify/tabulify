package net.bytle.db.move;

import net.bytle.db.spi.DataPath;

public class MoveProperties {

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
     *
     * @return a {@link MoveProperties} instance
     */
    public static MoveProperties of() {
        return new MoveProperties();
    }

    /**
     *
     * @param queueSize - The size of the buffer queue between the source and the target
     * @return the {@link MoveProperties} instance itself for chaining instantiation
     */
    public MoveProperties setQueueSize(Integer queueSize) {
        this.queueSize = queueSize;
        return this;
    }

    /**
     *
     * @param targetWorkerCount - The number of threads against the target data store
     * @return the {@link MoveProperties} instance itself for chaining instantiation
     */
    public MoveProperties setTargetWorkerCount(int targetWorkerCount) {
        this.targetWorkCount = targetWorkerCount;
        return this;
    }

    /**
     *
     * @param metricsDataPath - The location of the metrics data (ie snapshot of the counters by time)
     * @return the {@link MoveProperties} instance itself for chaining instantiation
     */
    public MoveProperties setMetricsPath(DataPath metricsDataPath) {
        this.metricsPath = metricsDataPath;
        return this;
    }

    /**
     *
     * @param fetchSize - The fetch size use by the source worker thread to retrieve data
     * @return the {@link MoveProperties} instance itself for chaining instantiation
     */
    public MoveProperties setFetchSize(Integer fetchSize) {
        this.fetchSize = fetchSize;
        return this;
    }

    /**
     *
     * @param batchSize The batch size is the buffer unit of the target worker.
     * @return the {@link MoveProperties} instance itself for chaining instantiation
     *
     * When the number of records fetched form the {@link #setQueueSize(Integer)}  | queue} exceeds the batch size, the data is send through the network
     * to the target data store
     */
    public MoveProperties setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    /**
     *
     * @param commitFrequency
     * @return the {@link MoveProperties} instance itself for chaining instantiation
     *
     * When the number of records fetched form the {@link #setQueueSize(Integer)}  | queue} exceeds the commit frequency size, a commit is send through the network
     * to the target data store.
     *
     * A commit can be considered the same than a flush statement on a file system.
     *
     */
    public MoveProperties setCommitFrequency(Integer commitFrequency) {
        this.commitFrequency = commitFrequency;
        return this;
    }
}
