package com.tabulify.flow.engine;

import java.util.List;

public interface PipelineStep {


  List<PipelineStep> getOnFailedOperations();

  List<PipelineStep> getOnCompletedOperations();

  List<PipelineStep> getDownStreamSteps();

}
