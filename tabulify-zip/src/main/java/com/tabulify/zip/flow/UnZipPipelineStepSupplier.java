package com.tabulify.zip.flow;

import com.tabulify.flow.engine.PipelineStepConsumerSupplierBuilderAbs;
import com.tabulify.flow.engine.PipelineStepIntermediateSupplier;
import com.tabulify.flow.engine.PipelineStepSupplierDataPathAbs;
import com.tabulify.flow.operation.StepOutputArgument;
import com.tabulify.fs.FsDataPath;
import com.tabulify.spi.DataPath;
import com.tabulify.zip.api.ArchiveEntry;
import com.tabulify.zip.api.ArchiveIterator;
import com.tabulify.zip.datapath.ArchiveDataPath;
import net.bytle.type.KeyNormalizer;

import java.nio.file.Path;

public class UnZipPipelineStepSupplier extends PipelineStepSupplierDataPathAbs {

  private final UnZipPipelineStepIntermediateOneToMany unzipOneToManyStep;
  private final ArchiveIterator archiveIterator;
  private final StepOutputArgument output;
  private final UnZipPipelineStep unZipPipelineStep;
  private final ArchiveDataPath archiveDataPath;

  public UnZipPipelineStepSupplier(PipelineStepSupplierBuilder pipelineStepSupplierBuilder) {
    super(pipelineStepSupplierBuilder);
    this.unzipOneToManyStep = pipelineStepSupplierBuilder.unZipOneToManyStep;
    this.unZipPipelineStep = this.unzipOneToManyStep.getUnZipPipelineStep();
    this.output = unZipPipelineStep.getOutputType();
    this.archiveDataPath = pipelineStepSupplierBuilder.archiveDataPath;
    this.archiveIterator = unZipPipelineStep.getIterator(archiveDataPath);

  }

  public static PipelineStepSupplierBuilder builder() {
    return new PipelineStepSupplierBuilder();
  }

  @Override
  public boolean hasNext() {
    return this.archiveIterator.hasNext();
  }


  @Override
  public PipelineStepIntermediateSupplier getIntermediateSupplier() {
    return this.unzipOneToManyStep;
  }

  @Override
  public DataPath get() {

    /**
     * Move the pointer to the next archive entry
     */
    ArchiveEntry entry = archiveIterator.next();

    /**
     * Get the destination
     */
    FsDataPath destinationDataPath = this.unZipPipelineStep.getTargetPath(archiveDataPath, entry);

    /**
     * Copy into the path
     */
    Path absoluteNioPath = destinationDataPath.getAbsoluteNioPath();
    archiveIterator.copyCurrentEntryToPath(absoluteNioPath);

    /**
     * Output
     */
    switch (this.output) {
      case RESULTS:
        return this.unZipPipelineStep.getResultDataPath(this.archiveDataPath, null)
          .getInsertStream()
          .insert(this.unZipPipelineStep.getResultsRecord(entry, destinationDataPath))
          .getDataPath();
      case TARGETS:
        return destinationDataPath;
      case INPUTS:
      default:
        throw new InternalError("The output type " + this.output + " is unexpected for the unzip split step.");
    }

  }

  public static class PipelineStepSupplierBuilder extends PipelineStepConsumerSupplierBuilderAbs {
    public static final KeyNormalizer UNZIP_SUPPLIER = KeyNormalizer.createSafe("unzip-supplier");
    private UnZipPipelineStepIntermediateOneToMany unZipOneToManyStep;
    private ArchiveDataPath archiveDataPath;

    @Override
    public UnZipPipelineStepSupplier build() {
      return new UnZipPipelineStepSupplier(this);
    }

    @Override
    public KeyNormalizer getOperationName() {
      return UNZIP_SUPPLIER;
    }

    public PipelineStepSupplierBuilder setUnzipOneToManyStep(UnZipPipelineStepIntermediateOneToMany unzipStep) {
      this.unZipOneToManyStep = unzipStep;
      return this;
    }

    public PipelineStepSupplierBuilder setArchiveDataPath(ArchiveDataPath dataPath) {
      this.archiveDataPath = dataPath;
      return this;
    }
  }
}
