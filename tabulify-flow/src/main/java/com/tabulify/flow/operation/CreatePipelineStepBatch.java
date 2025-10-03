package com.tabulify.flow.operation;

import com.tabulify.flow.engine.PipelineStepIntermediateManyToManyAbs;
import com.tabulify.flow.engine.PipelineStepSupplierDataPath;
import com.tabulify.spi.DataPath;

import java.util.ArrayList;
import java.util.List;

public class CreatePipelineStepBatch extends PipelineStepIntermediateManyToManyAbs {


  private final CreatePipelineStep builder;
  private List<DataPath> acceptedDataPathList = new ArrayList<>();

  public CreatePipelineStepBatch(CreatePipelineStep pipelineStepBuilder) {
    super(pipelineStepBuilder);
    this.builder = pipelineStepBuilder;
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
    acceptedDataPathList.add(dataPath);
  }

  @Override
  public PipelineStepSupplierDataPath get() {
    List<DataPath> outputs = new CreatePipelineStepFunction(this.builder).apply(acceptedDataPathList);
    return (PipelineStepSupplierDataPath) DefinePipelineStep.builder()
      .addDataPaths(outputs)
      .setIntermediateSupplier(this)
      .build();
  }

}
