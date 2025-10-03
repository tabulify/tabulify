package com.tabulify.transfer;

import com.tabulify.engine.ThreadListener;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.InsertStreamListener;
import com.tabulify.stream.SelectStream;
import com.tabulify.stream.Stream;

/**
 *
 * A transfer listener stream is a listener for a transfer
 * that use the streams (ie {@link SelectStream} and {@link InsertStream}
 * as opposed to the transfer that uses the data system capability (for instance Files.copy)
 *
 * This {@link TransferListener} can hold feedback information of the {@link Stream streams}
 *
 *
 */
public class TransferListenerStream extends TransferListenerAbs implements ThreadListener, TransferListener {



  private InsertStreamListener insertListener;


  public TransferListenerStream(TransferSourceTargetOrder transferSourceTarget) {
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
