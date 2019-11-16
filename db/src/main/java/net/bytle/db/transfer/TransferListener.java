package net.bytle.db.transfer;

import net.bytle.db.engine.ThreadListener;
import net.bytle.db.engine.ThreadListenerAbs;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.InsertStreamListener;
import net.bytle.db.stream.SelectStreamListener;
import net.bytle.timer.Timer;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by gerard on 12-01-2017.
 * An object listener to information from the threads
 * <p>
 * Example:
 * - exception and errors
 * - number of commits
 * - ...
 */
public class TransferListener extends ThreadListenerAbs implements ThreadListener {


    /**
     * The insert listeners are read to give live feedback
     * because they are also written, we make them thread safe with the synchronizedList
     */
    private List<InsertStreamListener> insertListener = Collections.synchronizedList(new ArrayList<>());
    private List<SelectStreamListener> selectListener = new ArrayList<>();

    private Timer timer = Timer.getTimer("total");

    public static TransferListener of() {
        return new TransferListener();
    }


    /**
     * The exit status:
     * - 0 if no errors occurs
     * - n: the number of exceptions otherwise
     *
     * @return
     */
    public int getExitStatus() {

        throw new RuntimeException("");

    }

    /**
     * The number of commit performed
     */
    public void incrementCommit() {



    }

    /**
     * The number of batch executed
     */
    public void incrementBatch() {


    }

    /**
     * The number of rows processed
     *
     * @param rows The number of records added
     */
    public void addRows(int rows) {



    }

    public Integer getCommits() {
        return null;
    }

    public Integer getRowCount() {
        return null;
    }

    public Integer getBatchCount() {
        return null;
    }


    public TransferListener addInsertListener(InsertStreamListener listener) {
        this.insertListener.add(listener);
        return this;
    }

    public TransferListener addSelectListener(SelectStreamListener selectStreamListener) {
        this.selectListener.add(selectStreamListener);
        return this;
    }

    public List<InsertStreamListener> getInsertStreamListeners() {
        return this.insertListener;
    }

    public void stopTimer() {
        timer.stop();
    }

    public void startTimer() {
        timer.start();
    }
}
