package com.tabulify.flow.operation;

import com.tabulify.flow.engine.PipelineStepIntermediateMapAbs;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DropTruncateAttribute;
import com.tabulify.spi.Tabulars;

public class DropPipelineStream extends PipelineStepIntermediateMapAbs {


  protected final DropPipelineStep dropBuilder;


  public DropPipelineStream(DropPipelineStep dropPipelineStep) {
    super(dropPipelineStep);
    this.dropBuilder = dropPipelineStep;
  }


  @Override
  public DataPath apply(DataPath dataPath) {


    Tabulars.drop(dataPath, this.dropBuilder.dropAttributes.toArray(DropTruncateAttribute[]::new));

    return dataPath;

  }


}
