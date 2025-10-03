package com.tabulify.transfer;

import com.tabulify.stream.Stream;

/**
 * This is a class to get the transfer statistics
 * from transfer operations that are not using the {@link Stream}
 *
 * This is the case in sql for instance with a `insert into`
 * There is only one operation
 *
 * FYI: The transfer listener from stream is {@link TransferListenerStream}
 *
 */
public class TransferListenerAtomic extends TransferListenerAbs {


  private long rows;

  public TransferListenerAtomic(TransferSourceTargetOrder transferSourceTarget) {
    super(transferSourceTarget);
  }

  @Override
  public int getCommits() {
    return 1;
  }

  @Override
  public Long getRowCount() {
    return rows;
  }

  @Override
  public int getBatchCount() {
    return 1;
  }

  public TransferListenerAtomic incrementRows(int rows) {
    this.rows = this.rows + rows;
    return this;
  }

}
