package com.tabulify.flow.engine;

import net.bytle.type.AttributeValue;

public abstract class FilterStepAbs extends StepAbs implements FilterOperationStep {


  @Override
  public boolean isAccumulator() {
    return false;
  }


  @Override
  public String toString() {
    return getName()+" ("+getOperationName()+")";
  }

  @Override
  public AttributeValue getOutput() {
    throw new RuntimeException("output is not implemented for the step "+this);
  }

  @Override
  public OperationStep setOutput(AttributeValue attribute) {
    throw new RuntimeException("output is not implemented for the step "+this);
  }
}
