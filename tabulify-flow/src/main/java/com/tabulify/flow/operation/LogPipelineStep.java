package com.tabulify.flow.operation;

import com.tabulify.flow.engine.PipelineStep;
import com.tabulify.flow.engine.PipelineStepBuilder;
import com.tabulify.flow.engine.PipelineStepIntermediateMapAbs;
import com.tabulify.spi.DataPath;
import net.bytle.type.KeyNormalizer;

public class LogPipelineStep extends PipelineStepIntermediateMapAbs {

  static final KeyNormalizer LOG = KeyNormalizer.createSafe("LOG");

  public LogPipelineStep(PipelineStepBuilder pipelineStepBuilder) {
    super(pipelineStepBuilder);
  }

  @Override
  public DataPath apply(DataPath dataPath) {
    System.out.println("DownStream Counter: " + this.getPipeline().getPipelineResult().getDownStreamCounter());
    return dataPath;
  }

  public static LogPipelineStepBuilder builder() {
    return new LogPipelineStepBuilder();
  }

  public static class LogPipelineStepBuilder extends PipelineStepBuilder {
    @Override
    public PipelineStepBuilder createStepBuilder() {
      return new LogPipelineStepBuilder();
    }

    @Override
    public PipelineStep build() {
      return new LogPipelineStep(this);
    }

    @Override
    public KeyNormalizer getOperationName() {
      return LOG;
    }
  }
}
