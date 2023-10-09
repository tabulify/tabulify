package net.bytle.db.flow.engine;

import net.bytle.type.AttributeValue;

import java.util.Set;

/**
 * A filter operation takes an input and returns an output
 */
public interface FilterOperationStep extends OperationStep {

  /**
   * A runnable object permits to run the operation on one item or on the whole flow
   * The {@link #isAccumulator()} function tells the flow engine if all data paths
   * should be given to {@link FilterRunnable#addInput(Set)}
   *
   * @return the object to run
   */
  FilterRunnable createRunnable();

  /**
   * If this function returns true, the {@link FilterRunnable#addInput(Set)}
   * will receive the whole flow
   * @return if this is an accumulator step
   */
  boolean isAccumulator();

  /**
   * @param attribute - the type of data path output that the steps should return
   * @return the step
   */
  OperationStep setOutput(AttributeValue attribute);

  /**
   *
   * @return the type of data path output that the steps should return
   */
  AttributeValue getOutput();


}
