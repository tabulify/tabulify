package net.bytle.db.flow.engine;

import net.bytle.db.spi.DataPath;

import java.util.ArrayList;
import java.util.List;

public class Message {


  /**
   * A trail of the operation, the message go through
   */
  private List<MessageTrail> steps = new ArrayList<>();

  /**
   * The wrapped data path
   */
  private DataPath dataPath;


  public Message(DataPath dataPath) {
    this.dataPath = dataPath;
  }

  public static Message create(DataPath dp) {
    return new Message(dp);
  }



  public MessageTrail createStep(OperationStep operationStep) {
    MessageTrail messageTrail = MessageTrail.create(operationStep);
    steps.add(messageTrail);
    messageTrail.start();
    return messageTrail;
  }

  public DataPath getDataPath() {
    return this.dataPath;
  }

  public MessageTrail getCurrentStep() {
    return steps.get(steps.size()-1);
  }
}
