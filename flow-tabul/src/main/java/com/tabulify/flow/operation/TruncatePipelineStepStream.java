package com.tabulify.flow.operation;

import com.tabulify.flow.engine.PipelineStepIntermediateMapAbs;
import com.tabulify.spi.DataPath;

import java.util.List;

public class TruncatePipelineStepStream extends PipelineStepIntermediateMapAbs {

  private final TruncatePipelineStepBuilder truncatePipelineStepBuilder;

  public TruncatePipelineStepStream(TruncatePipelineStepBuilder truncatePipelineStepBuilder) {
    super(truncatePipelineStepBuilder);
    this.truncatePipelineStepBuilder = truncatePipelineStepBuilder;
  }

  @Override
  public DataPath apply(DataPath dataPath) {

    dataPath.getConnection().getDataSystem().truncate(List.of(dataPath), this.truncatePipelineStepBuilder.truncateAttributes);
    return dataPath;
  }
}
