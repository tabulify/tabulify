package com.tabulify.flow.operation;

import com.tabulify.flow.engine.PipelineStepIntermediateManyToManyAbs;
import com.tabulify.flow.engine.PipelineStepSupplierDataPath;
import com.tabulify.spi.DataPath;

import java.util.ArrayList;
import java.util.List;

public class DependencyPipelineStepBatch extends PipelineStepIntermediateManyToManyAbs {
  private final DependencyPipelineStep dependencyBuilder;

  private List<DataPath> dataPathList = new ArrayList<DataPath>();

  public DependencyPipelineStepBatch(DependencyPipelineStep dependencyPipelineStep) {
    super(dependencyPipelineStep);
    this.dependencyBuilder = dependencyPipelineStep;
  }

  @Override
  public void reset() {
    dataPathList = new ArrayList<>();
  }

  @Override
  public List<DataPath> getDataPathsBuffer() {
    return dataPathList;
  }

  @Override
  public void accept(DataPath dataPath) {
    this.dataPathList.add(dataPath);
  }

  @Override
  public PipelineStepSupplierDataPath get() {

    DataPath dataPath = this.dependencyBuilder.getDependencyDataPath(this.dataPathList);
    this.reset();
    return (PipelineStepSupplierDataPath) DefinePipelineStep.builder()
      .addDataPath(dataPath)
      .setIntermediateSupplier(this)
      .setPipeline(this.getPipeline())
      .build();

  }
}
