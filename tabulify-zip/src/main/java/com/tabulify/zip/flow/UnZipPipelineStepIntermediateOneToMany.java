package com.tabulify.zip.flow;

import com.tabulify.flow.engine.PipelineStepIntermediateOneToManyAbs;
import com.tabulify.spi.DataPath;
import com.tabulify.zip.datapath.ArchiveDataPath;

public class UnZipPipelineStepIntermediateOneToMany extends PipelineStepIntermediateOneToManyAbs {
  private final UnZipPipelineStep unzipPipelineStep;

  public UnZipPipelineStepIntermediateOneToMany(UnZipPipelineStep unZipPipelineStepBuilder) {
    super(unZipPipelineStepBuilder);
    this.unzipPipelineStep = unZipPipelineStepBuilder;
  }

  @Override
  public UnZipPipelineStepSupplier apply(DataPath inputDataPath) {
    if (!(inputDataPath instanceof ArchiveDataPath)) {
      throw new IllegalArgumentException("The input data resource (" + inputDataPath + ") is not an archive but a " + inputDataPath.getMediaType());
    }
    return (UnZipPipelineStepSupplier) UnZipPipelineStepSupplier.builder()
      .setUnzipOneToManyStep(this)
      .setArchiveDataPath((ArchiveDataPath) inputDataPath)
      .setPipeline(this.getPipeline())
      .build();
  }

  public UnZipPipelineStep getUnZipPipelineStep() {
    return this.unzipPipelineStep;
  }
}
