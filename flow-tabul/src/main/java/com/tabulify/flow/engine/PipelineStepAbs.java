package com.tabulify.flow.engine;

import com.tabulify.type.KeyNormalizer;


public abstract class PipelineStepAbs extends ExecutionNodeAbs implements PipelineStep {


  private final PipelineStepBuilder stepBuilder;


  public PipelineStepAbs(PipelineStepBuilder stepBuilder) {
    super(stepBuilder);
    this.stepBuilder = stepBuilder;
  }


  @Override
  public String toString() {
    return this.stepBuilder.getNodeName() + " (" + getOperationName() + ")";
  }


  @Override
  public KeyNormalizer getOperationName() {
    return stepBuilder.getOperationName();
  }


  @Override
  public Integer getNodeId() {
    return getPipelineStepId();
  }

  @Override
  public Pipeline getPipeline() {
    return stepBuilder.getPipeline();
  }


  @Override
  public Integer getPipelineStepId() {
    return stepBuilder.getPipelineStepId();
  }

  @Override
  public String getNodeType() {

    if (this instanceof PipelineStepRoot) {
      return "Supplier";
    }
    if (this instanceof PipelineStepIntermediateMap) {
      if (this instanceof PipelineStepIntermediateMapNullable) {
        /**
         * One to null
         */
        return "Filter";
      }
      /**
       * One to one
       */
      return "Map";
    }
    if (this instanceof PipelineStepIntermediateOneToMany) {
      return "Split";
    }
    if (this instanceof PipelineStepIntermediateManyToManyAbs) {
      return "Collector";
    }
    return super.getNodeType();
  }


}
