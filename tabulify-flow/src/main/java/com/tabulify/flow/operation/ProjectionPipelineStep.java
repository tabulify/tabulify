package com.tabulify.flow.operation;

import com.tabulify.flow.engine.PipelineStepBuilderTarget;
import com.tabulify.flow.engine.PipelineStepIntermediateMapAbs;
import com.tabulify.spi.DataPath;
import net.bytle.type.KeyNormalizer;

import java.util.List;

/**
 * Return a data resources filtered by columns
 */
public class ProjectionPipelineStep extends PipelineStepIntermediateMapAbs {

  private final ProjectionPipelineStepBuilder projectionBuilder;

  public ProjectionPipelineStep(ProjectionPipelineStepBuilder projectionPipelineStepBuilder) {
    super(projectionPipelineStepBuilder);
    this.projectionBuilder = projectionPipelineStepBuilder;
  }

  public static ProjectionPipelineStepBuilder builder() {
    return null;
  }

  @Override
  public DataPath apply(DataPath dataPath) {
    return null;
  }

  public static class ProjectionPipelineStepBuilder extends PipelineStepBuilderTarget {
    static final KeyNormalizer PROJECTION = KeyNormalizer.createSafe("projection");
    private List<Integer> columnByPosition = null;

    @Override
    public ProjectionPipelineStepBuilder createStepBuilder() {
      return new ProjectionPipelineStepBuilder();
    }

    @Override
    public ProjectionPipelineStep build() {
      return new ProjectionPipelineStep(this);
    }

    @Override
    public KeyNormalizer getOperationName() {
      return PROJECTION;
    }

    public ProjectionPipelineStepBuilder setColumnByPosition(List<Integer> columnMapping) {


      this.columnByPosition = columnMapping;

      return this;
    }

  }
}
