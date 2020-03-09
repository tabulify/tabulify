package net.bytle.db.transfer;

import net.bytle.db.engine.ThreadListener;

/**
 * A transfer listeners return statistics / information about the
 * transfers.
 *
 * Example:
 *  * exception and errors
 *  * number of commits
 *  * number of rows
 *  * histogram (not yet implemented)
 */
public interface TransferListener extends Comparable<TransferListener>, ThreadListener {

  int getExitStatus();

  int getCommits();

  long getRowCount();

  int getBatchCount();

  TransferListener stopTimer();

  TransferListener startTimer();

  long getResponseTime();

  String getErrorMessage();

  TransferSourceTarget getTransferSourceTarget();

}
