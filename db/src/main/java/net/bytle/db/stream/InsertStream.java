package net.bytle.db.stream;

import net.bytle.db.spi.DataPath;

import java.util.List;

public interface InsertStream extends AutoCloseable {


    /**
     * @return the name of this stream
     * The table name if not set
     */
    String getName();

    /**
     * @param name - The name of this stream to be able to identify it in threads
     * @return the {@link InsertStream} for constructor chaining
     */
    InsertStream setName(String name);

    /**
     * @param values - The values to insert
     * @return the {@link InsertStream} for insert chaining
     */
    InsertStream insert(Object... values);

    /**
     *
     * @param values - The values to insert in a positional order
     * @return the {@link InsertStream} for insert chaining
     */
    InsertStream insert(List<Object> values);

    /**
     *
     * @param feedbackFrequency - the number of output by batch size
     * @return the {@link InsertStream} for constructor chaining
     */
    InsertStream setFeedbackFrequency(Integer feedbackFrequency);

    /**
     * To of feedback and to retrieve the exception if the input stream
     * is wrappeds in a thread
     * @return insertStreamListener or null if not implemented
     */
    InsertStreamListener getInsertStreamListener();

    /**
     * Close the stream
     * ie
     * * commit the last rows
     * * close the connection
     * * ...
     */
    void close();

    /**
     * The commit frequency based on the batch size
     * <p>
     * 3 means that every 3 batch the data will be commited (flushed)
     *
     * @param commitFrequency
     * @return the {@link InsertStream} for constructor chaining
     */
    InsertStream setCommitFrequency(Integer commitFrequency);

    /**
     * The number of rows that defined the buffer size (batch in JDBC)
     *
     * @param batchSize
     * @return the {@link InsertStream} for constructor chaining
     */
    InsertStream setBatchSize(Integer batchSize);

    /**
     * @return the data path Definition
     */
    DataPath getDataPath();

    /**
     * Does the next insert will send a batch
     *
     * @return
     */
    boolean flushAtNextInsert();

    /**
     * In case of parent child hierarchy
     * we can check if we need to send the data with the function nextInsertSendBatch()
     * and send it with this function
     */
    void flush();
}
