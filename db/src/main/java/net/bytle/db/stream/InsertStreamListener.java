package net.bytle.db.stream;

import net.bytle.db.engine.ThreadListener;
import net.bytle.db.engine.ThreadListenerAbs;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gerard on 12-01-2017.
 * An object listener to of informations from the threads
 * <p>
 * Example:
 * - exception and errors
 * - number of commits
 * - ...
 */
public class InsertStreamListener extends ThreadListenerAbs implements ThreadListener {

    private InsertStream insertStream;

    private Integer commits = 0;
    private Integer rows = 0;
    private Integer batches = 0;
    private List<RuntimeException> exceptions = new ArrayList<>();

    private InsertStreamListener(InsertStream insertStream) {

        this.insertStream = insertStream;

    }


    public static InsertStreamListener get(InsertStream insertStream) {
        return new InsertStreamListener(insertStream);
    }


    /**
     * The exit status:
     * - 0 if no errors occurs
     * - n: the number of exceptions otherwise
     *
     * @return
     */
    public int getExitStatus() {

        return exceptions.size();

    }

    /**
     * The number of commit performed
     */
    public void incrementCommit() {

        this.commits++;

    }

    /**
     * The number of batch executed
     */
    public void incrementBatch() {

        this.batches++;

    }

    /**
     * The number of rows processed
     *
     * @param rows The number of records added
     */
    public void addRows(int rows) {

        this.rows = this.rows + rows;

    }

    public Integer getCommits() {
        return commits;
    }

    public Integer getRowCount() {
        return rows;
    }

    public Integer getBatchCount() {
        return batches;
    }


    public InsertStream getInsertStream() {
        return insertStream;
    }
}
