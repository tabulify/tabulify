package com.tabulify.flow.engine;

import net.bytle.timer.Timer;

/**
 * The trail of the message in the pipeline
 */
public class MessageTrail {

  /**
   * The step operation
   */
  private final OperationStep operationStep;

  private OperationStatus operationStatus = OperationStatus.OPEN;
  private String statusMessage;

  /**
   * A timer for the whole step
   */
  private Timer timer = Timer.create("step");

  public MessageTrail(OperationStep operationStep) {
    this.operationStep = operationStep;
  }

  public static MessageTrail create(OperationStep operationStep) {
    return new MessageTrail(operationStep);
  }


  public MessageTrail setOperationStatus(OperationStatus operationStatus) {
    this.operationStatus = operationStatus;
    this.timer.stop();
    return this;
  }

  public MessageTrail setOperationStatusMessage(String s) {
    this.statusMessage = s;
    return this;
  }


  public MessageTrail start() {
    this.timer.start();
    return this;
  }

}
