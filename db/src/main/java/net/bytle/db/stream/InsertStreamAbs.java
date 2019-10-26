package net.bytle.db.stream;

import net.bytle.db.spi.DataPath;

import java.util.Arrays;
import java.util.List;

public abstract class InsertStreamAbs implements InsertStream, AutoCloseable {

    private final DataPath dataPath;
    protected InsertStreamListener insertStreamListener = InsertStreamListener.get(this);

    protected String name = Thread.currentThread().getName();
    protected Integer feedbackFrequency = 10000;
    protected Integer batchSize = 10000;
    protected Integer commitFrequency = 100;
    protected int currentRowInLogicalBatch = 0;

    public InsertStreamAbs(DataPath dataPath) {

        this.dataPath = dataPath;
    }

    @Override
    public InsertStream setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public InsertStream insert(Object... values) {
        List<Object> valuesToInsert = Arrays.asList(values);

        // Case when the first object is a list
        if (values.length == 1 && values[0] instanceof List) {
            valuesToInsert = (List<Object>) values[0];
        }

        return this.insert(valuesToInsert);
    }

    @Override
    public InsertStream setFeedbackFrequency(Integer feedbackFrequency) {
        this.feedbackFrequency = feedbackFrequency;
        return this;
    }

    @Override
    public InsertStreamListener getInsertStreamListener() {
        return this.insertStreamListener;
    }

    @Override
    public InsertStream setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    @Override
    public InsertStream setCommitFrequency(Integer commitFrequency) {
        this.commitFrequency = commitFrequency;
        return this;
    }

    @Override
    public DataPath getDataPath() {
        return dataPath;
    }


    /**
     * Does the next insert will send data (ie a batch)
     * to the remote server
     *
     * @return
     */
    @Override
    public boolean flushAtNextInsert() {
        return (currentRowInLogicalBatch + 1 == batchSize);
    }

}
