package com.tabulify.flow.engine;

import com.tabulify.flow.operation.PipelineStepProcessingType;
import com.tabulify.spi.DataPath;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A supplier step that:
 * * collects data path
 * * then supply a supplier
 * <p>
 * We call it a {@link PipelineStepAbs#getNodeType()} collector because it will collect many data path.
 * <p>
 * In a {@link PipelineStepRoot#getProcessingType()} stream context
 * a collector is called at the {@link Pipeline#getWindowInterval() window interval} to release the collection
 */
public abstract class PipelineStepIntermediateManyToManyAbs extends PipelineStepAbs implements PipelineStepIntermediateSupplier, Consumer<DataPath>, Supplier<PipelineStepSupplierDataPath> {

  public PipelineStepIntermediateManyToManyAbs(PipelineStepBuilder pipelineStepBuilder) {
    super(pipelineStepBuilder);
  }

  /**
   * Reset the state
   * When the pipeline is a {@link PipelineStepRootStreamSupplier}
   * The collector state is reset after each get of the {@link PipelineStepSupplierDataPath}
   */
  abstract public void reset();

  @Override
  public PipelineStepProcessingType getProcessingType() {
    // batch processing, they are executed at interval
    return PipelineStepProcessingType.BATCH;
  }

  /**
   * @return the actual data paths accepted
   * It's used to move them if any error occurs
   */
  public abstract List<DataPath> getDataPathsBuffer();
}
