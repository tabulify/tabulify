package com.tabulify.flow.operation;

import com.tabulify.flow.engine.PipelineCascadeNode;
import com.tabulify.flow.engine.PipelineStepBuilder;
import com.tabulify.flow.engine.PipelineStepIntermediateMapNullableAbs;
import com.tabulify.spi.DataPath;
import com.tabulify.type.KeyNormalizer;

import java.util.HashSet;
import java.util.Set;

/**
 * A distinct operation to return only a unique set of data path
 */
public class DistinctPipelineStep extends PipelineStepIntermediateMapNullableAbs {


  private final Set<DataPath> dataPathSet = new HashSet<>();
  protected final DistinctPipelineStepBuilder distinctStepBuilder;


  public DistinctPipelineStep(DistinctPipelineStepBuilder distinctStepBuilder) {
    super(distinctStepBuilder);
    this.distinctStepBuilder = distinctStepBuilder;
  }


  public static DistinctPipelineStepBuilder builder() {
    return new DistinctPipelineStepBuilder();
  }

  @Override
  public DataPath apply(DataPath dataPath) {

    /**
     * Synchronized because:
     * Caused by: java.util.ConcurrentModificationException: Error in the step step5 (distinct).
     * at java.base/java.util.HashMap$HashIterator.nextNode(HashMap.java:1511)
     * at java.base/java.util.HashMap$KeyIterator.next(HashMap.java:1534)
     * <p>
     * The only schedule is here, I don't see how it could introduce concurrency
     * We use {@link PipelineCascadeNode#scheduleChildIntermediateCollectorNodeAtInterval()}
     */
    synchronized (this.dataPathSet) {
      if (this.dataPathSet.contains(dataPath)) {
        return null;
      }
      this.dataPathSet.add(dataPath);
    }

    return dataPath;
  }


  public static class DistinctPipelineStepBuilder extends PipelineStepBuilder {

    static final KeyNormalizer DISTINCT = KeyNormalizer.createSafe("distinct");


    @Override
    public DistinctPipelineStepBuilder createStepBuilder() {
      return new DistinctPipelineStepBuilder();
    }


    @Override
    public DistinctPipelineStep build() {
      return new DistinctPipelineStep(this);
    }


    @Override
    public KeyNormalizer getOperationName() {
      return DISTINCT;
    }

  }
}
