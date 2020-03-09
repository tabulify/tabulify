package net.bytle.db.transfer;


import net.bytle.db.engine.ThreadListenerAbs;
import net.bytle.timer.Timer;

/**
 * The base implementation of a {@link TransferListener}
 *
 */
public abstract class TransferListenerAbs extends ThreadListenerAbs implements TransferListener {

  //The source target of this transfer
  private final TransferSourceTarget transferSourceTarget;

  private Timer timer = Timer.getTimer("total");

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
  public long getResponseTime() {
    return timer.getResponseTimeInMilliSeconds();
  }




}
