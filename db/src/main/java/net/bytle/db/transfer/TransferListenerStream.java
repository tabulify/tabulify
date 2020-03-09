package net.bytle.db.transfer;

import net.bytle.db.engine.ThreadListener;
import net.bytle.db.stream.InsertStreamListener;
import net.bytle.db.stream.SelectStreamListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An {@link TransferListener} to get information from a transfer with {@link net.bytle.db.stream.Stream streams}
 *
 *
 */
public class TransferListenerStream extends TransferListenerAbs implements ThreadListener, TransferListener {



  /**
   * The insert listeners are read to give live feedback
   * because they are also written, we make them thread safe with the synchronizedList
   */
  private List<InsertStreamListener> insertListener = Collections.synchronizedList(new ArrayList<>());
  private List<SelectStreamListener> selectListener = new ArrayList<>();



  public TransferListenerStream(TransferSourceTarget transferSourceTarget) {
    super(transferSourceTarget);
  }



  /**
   * The exit status:
   * - 0 if no errors occurs
   * - n: the number of exceptions otherwise
   *
   * @return
   */
  @Override
  public int getExitStatus() {

    return getExceptions().size();

  }



  @Override
  public int getCommits() {
    return insertListener.stream().mapToInt(InsertStreamListener::getCommits).sum();
  }

  @Override
  public long getRowCount() {
    return insertListener.stream().mapToLong(InsertStreamListener::getRowCount).sum();
  }

  @Override
  public int getBatchCount() {
    return insertListener.stream().mapToInt(InsertStreamListener::getBatchCount).sum();
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


}
