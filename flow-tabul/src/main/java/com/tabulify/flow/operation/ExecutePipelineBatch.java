package com.tabulify.flow.operation;

import com.tabulify.flow.engine.PipelineStepIntermediateManyToManyAbs;
import com.tabulify.flow.engine.PipelineStepSupplierDataPath;
import com.tabulify.spi.DataPath;

import java.util.ArrayList;
import java.util.List;

public class ExecutePipelineBatch extends PipelineStepIntermediateManyToManyAbs {

  private final ExecutePipelineStep builder;
  private List<DataPath> acceptedDataPathList = new ArrayList<>();

  public ExecutePipelineBatch(ExecutePipelineStep execPipelineStep) {
    super(execPipelineStep);
    this.builder = execPipelineStep;
  }

  @Override
  public void reset() {
    acceptedDataPathList = new ArrayList<>();
  }

  @Override
  public List<DataPath> getDataPathsBuffer() {
    return acceptedDataPathList;
  }

  @Override
  public void accept(DataPath dataPath) {
    this.acceptedDataPathList.add(dataPath);
  }

  @Override
  public PipelineStepSupplierDataPath get() {

    DataPath result = this.builder.execute(acceptedDataPathList);

    return (PipelineStepSupplierDataPath) DefinePipelineStep
      .builder()
      .addDataPaths(List.of(result))
      .setIntermediateSupplier(this)
      .build();
  }
}
