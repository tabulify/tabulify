package com.tabulify.flow.operation;

import com.tabulify.flow.engine.PipelineStepIntermediateManyToManyAbs;
import com.tabulify.flow.engine.PipelineStepSupplierDataPath;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DropTruncateAttribute;
import com.tabulify.spi.Tabulars;

import java.util.ArrayList;
import java.util.List;

/**
 * Truncate is a set operation
 */
public class TruncatePipelineStepBatch extends PipelineStepIntermediateManyToManyAbs {


  private ArrayList<DataPath> dataPaths = new ArrayList<>();
  private final TruncatePipelineStepBuilder truncatePipelineStepBuilder;

  public TruncatePipelineStepBatch(TruncatePipelineStepBuilder operationProvider) {
    super(operationProvider);
    this.truncatePipelineStepBuilder = operationProvider;
  }


  public static TruncatePipelineStepBuilder builder() {
    return new TruncatePipelineStepBuilder();
  }


  @Override
  public void accept(DataPath dataPath) {


    this.dataPaths.add(dataPath);


  }


  public void truncate() {

    // Doing the work
    if (this.dataPaths.isEmpty()) {
      return;
    }
    Tabulars.truncate(this.dataPaths, this.truncatePipelineStepBuilder.truncateAttributes.toArray(new DropTruncateAttribute[0]));


  }


  @Override
  public void reset() {
    this.dataPaths = new ArrayList<>();
  }

  @Override
  public List<DataPath> getDataPathsBuffer() {
    return new ArrayList<>(dataPaths);
  }

  @Override
  public PipelineStepSupplierDataPath get() {
    this.truncate();
    return (PipelineStepSupplierDataPath) DefinePipelineStep.builder()
      .addDataPaths(this.dataPaths)
      .setIntermediateSupplier(this)
      .build();
  }
}
