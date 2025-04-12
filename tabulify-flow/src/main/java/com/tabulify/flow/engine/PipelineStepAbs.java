package com.tabulify.flow.engine;

import java.util.ArrayList;
import java.util.List;

public abstract class PipelineStepAbs implements PipelineStep {

  /**
   * This functions will get the sources that have failed
   *
   * If this is the load of a file:
   *   * you may want to move them
   *   * Send a error message
   *
   * Just a series of action for instance:
   *   * collect the list of files in a list
   *   * save it in excel format
   *   * and send it via an email
   */
  List<PipelineStep> postFailedOperations = new ArrayList<>();

  /**
   * All this functions will get the sources that have succeeded
   *
   * If this is the load of a file:
   *   * you may want to delete it (ie free the resources)
   *   * send a tracing event
   */
  List<PipelineStep> postCompleteOperations = new ArrayList<>();

  /**
   * The output of the step is send to one or more
   * play step, forming a tree
   * If this is only a series of step, this forms a pipeline
   */
  List<PipelineStep> downStreamSteps;


  @Override
  public List<PipelineStep> getOnFailedOperations() {

    return this.postFailedOperations;

  }

  @Override
  public List<PipelineStep> getOnCompletedOperations() {

    return this.postCompleteOperations;

  }

  @Override
  public List<PipelineStep> getDownStreamSteps() {

    return this.downStreamSteps;

  }

}
