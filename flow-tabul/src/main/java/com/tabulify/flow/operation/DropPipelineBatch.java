package com.tabulify.flow.operation;

import com.tabulify.engine.ForeignKeyDag;
import com.tabulify.flow.engine.PipelineStepIntermediateManyToManyAbs;
import com.tabulify.flow.engine.PipelineStepSupplierDataPath;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DropTruncateAttribute;
import com.tabulify.spi.Tabulars;

import java.util.ArrayList;
import java.util.List;

public class DropPipelineBatch extends PipelineStepIntermediateManyToManyAbs {

  private final DropPipelineStep builder;
  private List<DataPath> acceptedDataPathList = new ArrayList<>();

  public DropPipelineBatch(DropPipelineStep pipelineStepBuilder) {
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
    this.acceptedDataPathList.add(dataPath);
  }

  @Override
  public PipelineStepSupplierDataPath get() {


    List<DataPath> dataPaths;
    try {
      dataPaths = ForeignKeyDag.createFromPaths(acceptedDataPathList).getDropOrdered();
    } catch (Exception e) {
      // bad view
      dataPaths = acceptedDataPathList;
    }
    for (DataPath dataPath : dataPaths) {
      Tabulars.drop(dataPath, this.builder.dropAttributes.toArray(DropTruncateAttribute[]::new));
    }

    return (PipelineStepSupplierDataPath) DefinePipelineStep
      .builder()
      .addDataPaths(acceptedDataPathList)
      .setIntermediateSupplier(this)
      .build();
  }
}
