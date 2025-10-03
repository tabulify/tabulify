package com.tabulify.flow.operation;

import com.tabulify.flow.engine.PipelineStepIntermediateManyToManyAbs;
import com.tabulify.flow.engine.PipelineStepRootStreamSupplier;
import com.tabulify.flow.engine.PipelineStepSupplierDataPath;
import com.tabulify.spi.DataPath;

import java.util.ArrayList;
import java.util.List;

public class TransferPipelineStepBatch extends PipelineStepIntermediateManyToManyAbs {

  private final TransferPipelineStep transferPipelineStep;
  private List<DataPath> dataPaths = new ArrayList<>();

  public TransferPipelineStepBatch(TransferPipelineStep transferPipelineStep) {
    super(transferPipelineStep);
    this.transferPipelineStep = transferPipelineStep;
  }

  @Override
  public void accept(DataPath dataPath) {
    this.dataPaths.add(dataPath);
  }

  @Override
  public PipelineStepSupplierDataPath get() {
    /**
     * Do the work
     */
    List<DataPath> results = this.transferPipelineStep.apply(dataPaths);
    /**
     * Return
     */
    return (PipelineStepSupplierDataPath) DefinePipelineStep.builder()
      .addDataPaths(results)
      .setIntermediateSupplier(this)
      .setPipeline(this.getPipeline())
      .build();
  }

  @Override
  public void reset() {
    /**
     * Reset dataPaths if we are in a {@link PipelineStepRootStreamSupplier}
     */
    dataPaths = new ArrayList<>();
  }

  @Override
  public List<DataPath> getDataPathsBuffer() {
    return dataPaths;
  }

}
