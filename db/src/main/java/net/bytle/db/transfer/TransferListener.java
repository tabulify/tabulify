package net.bytle.db.transfer;

import net.bytle.db.engine.ThreadListener;
import net.bytle.db.engine.ThreadListenerAbs;
import net.bytle.db.stream.InsertStreamListener;
import net.bytle.db.stream.SelectStreamListener;
import net.bytle.timer.Timer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by gerard on 12-01-2017.
 * An object listener to information from the threads
 * <p>
 * Example:
 * - exception and errors
 * - number of commits
 * - ...
 */
public class TransferListener extends ThreadListenerAbs implements ThreadListener, Comparable<TransferListener> {

  //The source target of this transfer
  private final TransferSourceTarget transferSourceTarget;

  /**
   * The insert listeners are read to give live feedback
   * because they are also written, we make them thread safe with the synchronizedList
   */
  private List<InsertStreamListener> insertListener = Collections.synchronizedList(new ArrayList<>());
  private List<SelectStreamListener> selectListener = new ArrayList<>();

  private Timer timer = Timer.getTimer("total");

  public TransferListener(TransferSourceTarget transferSourceTarget) {
    this.transferSourceTarget = transferSourceTarget;
  }

  public static TransferListener of(TransferSourceTarget transferSourceTarget) {
    return new TransferListener(transferSourceTarget);
  }


  /**
   * The exit status:
   * - 0 if no errors occurs
   * - n: the number of exceptions otherwise
   *
   * @return
   */
  public int getExitStatus() {

    return getExceptions().size();

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

  public Long getRowCount() {
    return insertListener.stream().mapToLong(InsertStreamListener::getRowCount).sum();
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

  public TransferListener stopTimer() {
    timer.stop();
    return this;
  }

  public TransferListener startTimer() {
    timer.start();
    return this;
  }

  @Override
  public int compareTo(TransferListener o) {
    return this.transferSourceTarget.getSourceDataPath().compareTo(o.transferSourceTarget.getSourceDataPath());
  }

  public TransferSourceTarget getSourceTarget() {
    return this.transferSourceTarget;
  }

  public long getResponseTime() {
    return timer.getResponseTimeInMilliSeconds();
  }

  public String getErrorMessage() {
    return getExceptions()
      .stream()
      .map(Throwable::getMessage)
      .collect(Collectors.joining(", "));
  }
}
