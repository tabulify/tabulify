package com.tabulify.flow.operation;

import com.tabulify.flow.engine.PipelineStepIntermediateMapAbs;
import com.tabulify.spi.DataPath;

import java.util.List;

public class TransferPipelineStepStream extends PipelineStepIntermediateMapAbs {


  private final TransferPipelineStep transferPipelineStep;


  public TransferPipelineStepStream(TransferPipelineStep pipelineStepBuilder) {
    super(pipelineStepBuilder);
    this.transferPipelineStep = pipelineStepBuilder;
  }


  @Override
  public DataPath apply(DataPath sourceDataPath) {

    return transferPipelineStep
      .apply(List.of(sourceDataPath))
      .iterator()
      .next();

  }


}
