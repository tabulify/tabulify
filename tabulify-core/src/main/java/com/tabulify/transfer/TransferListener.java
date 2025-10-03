package com.tabulify.transfer;

import com.tabulify.engine.ThreadListener;
import net.bytle.timer.Timer;

import java.util.List;

/**
 * A transfer listeners return statistics / information about the
 * transfers.
 * <p>
 * Example:
 *  * exception and errors
 *  * number of commits
 *  * number of rows
 *  * histogram (not yet implemented)
 */
public interface TransferListener extends Comparable<TransferListener>, ThreadListener {

  /**
   *
   * @return the exit status
   */
  int getExitStatus();

  /**
   * The number of commit
   * @return
   */
  int getCommits();

  /**
   *
   * @return the number of records / rows count
   */
  Long getRowCount();

  /**
   *
   * @return the number of batch
   */
  int getBatchCount();

  /**
   * Stop the timer
   * @return
   */
  TransferListener stopTimer();

  /**
   * Start the timer
   * @return
   */
  TransferListener startTimer();

  /**
   *
   * @return
   */
  Timer getTimer();

  /**
   *
   * @return
   */
  List<String> getErrorMessages();

  /**
   *
   * @return the target data path, the source data path and the {@link TransferPropertiesCross}
   */
  TransferSourceTargetOrder getTransferSourceTarget();

  /**
   *
   * @return the {@link TransferMethod method} used to perform the {@link TransferOperation}
   */
  TransferMethod getMethod();

  /**
   * Add the fact that a data operation on the target data resource as occurred
   * @param transferResourceOperations
   * @return
   */
  TransferListener addTargetOperation(TransferResourceOperations transferResourceOperations);

  /**
   * Add the fact that a data operation on the source data resource as occurred
   * @param transferResourceOperations
   * @return
   */
  TransferListener addSourceOperation(TransferResourceOperations transferResourceOperations);

  /**
   *
   * @return the {@link TransferResourceOperations} that has occurred against the target
   */
  List<TransferResourceOperations> getTargetDataOperations();

  List<TransferResourceOperations> getSourceDataOperations();

  /**
   *
   * @param transferType - the type of transfer process (To control that the code is going in the right path)
   * @return
   */
  TransferListener setType(TransferType transferType);

  /**
   *
   * @return the type
   */
  TransferType getType();
}
