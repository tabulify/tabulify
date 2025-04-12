package com.tabulify.transfer;


import com.tabulify.engine.ThreadListenerAbs;
import net.bytle.timer.Timer;

import java.util.ArrayList;
import java.util.List;

/**
 * The base implementation of a {@link TransferListener}
 *
 */
public abstract class TransferListenerAbs extends ThreadListenerAbs implements TransferListener {

  //The source target of this transfer
  private final TransferSourceTarget transferSourceTarget;

  private Timer timer = Timer.create("total");

  /**
   * The transfer method used
   */
  private TransferMethod method;

  /**
   *
   * @param transferSourceTarget
   */
  private List<TransferResourceOperations> targetDataOperations = new ArrayList<>();
  private List<TransferResourceOperations> sourceDataOperations = new ArrayList<>();;
  private TransferType transferType;

  public TransferListenerAbs(TransferSourceTarget transferSourceTarget) {
    this.transferSourceTarget = transferSourceTarget;
  }
  @Override
  public TransferListener stopTimer() {
    timer.stop();
    return this;
  }

  @Override
  public TransferListener startTimer() {
    timer.start();
    return this;
  }

  @Override
  public int compareTo(TransferListener o) {
    return this.transferSourceTarget.getSourceDataPath().compareTo(o.getTransferSourceTarget().getSourceDataPath());
  }

  @Override
  public TransferSourceTarget getTransferSourceTarget() {
    return this.transferSourceTarget;
  }

  @Override
  public Timer getTimer() {
    return timer;
  }

  @Override
  public TransferMethod getMethod() {
    return this.method;
  }

  public TransferListener setMethod(TransferMethod transferMethod){
    this.method = transferMethod;
    return this;
  }

  @Override
  public TransferListener addTargetOperation(TransferResourceOperations transferResourceOperations){
    this.targetDataOperations.add(transferResourceOperations);
    return this;
  }

  @Override
  public TransferListener addSourceOperation(TransferResourceOperations transferResourceOperations){
    this.sourceDataOperations.add(transferResourceOperations);
    return this;
  }

  @Override
  public List<TransferResourceOperations> getTargetDataOperations() {
    return this.targetDataOperations;
  }

  @Override
  public List<TransferResourceOperations> getSourceDataOperations() {
    return this.sourceDataOperations;
  }

  @Override
  public TransferListener setType(TransferType transferType){
    this.transferType = transferType;
    return this;
  }

  @Override
  public TransferType getType() {
    return this.transferType;
  }
}
