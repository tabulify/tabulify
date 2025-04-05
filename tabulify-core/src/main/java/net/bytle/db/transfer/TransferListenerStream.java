package net.bytle.db.transfer;

import net.bytle.db.engine.ThreadListener;
import net.bytle.db.stream.InsertStreamListener;

/**
 *
 * A transfer listener stream is a listener for a transfer
 * that use the streams (ie {@link net.bytle.db.stream.SelectStream} and {@link net.bytle.db.stream.InsertStream}
 * as opposed to the transfer that uses the data system capability (for instance Files.copy)
 *
 * This {@link TransferListener} can hold feedback information of the {@link net.bytle.db.stream.Stream streams}
 *
 *
 */
public class TransferListenerStream extends TransferListenerAbs implements ThreadListener, TransferListener {



  private InsertStreamListener insertListener;


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
    return insertListener.getCommits();
  }

  @Override
  public Long getRowCount() {
    return insertListener.getRowCount();
  }

  @Override
  public int getBatchCount() {
    return insertListener.getBatchCount();
  }


  public TransferListener addInsertListener(InsertStreamListener insertStreamListener) {
    this.insertListener = insertStreamListener;
    return this;
  }


  public InsertStreamListener getInsertStreamListeners() {
    return this.insertListener;
  }




}
