package com.tabulify.flow.engine;

import com.tabulify.spi.DataPath;
import net.bytle.exception.InternalException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class PipelineStepResult implements Comparable<PipelineStepResult> {

  private final PipelineStepResultBuilder builder;
  private long inputCounter = 0;
  private long outputCounter = 0;


  private final List<DataPath> inputDataPaths = new ArrayList<>();
  private final List<DataPath> outputDataPaths = new ArrayList<>();

  /**
   * An execution is :
   * * for a supplier:
   * * stream: the number of {@link PipelineStepRootStreamSupplier#poll()} execution
   * * batch:  1 for a root {@link PipelineStepRootBatchSupplier}
   * * for a map
   * * 1 by map execution for a {@link PipelineStepIntermediateMap}
   * * for an intermediate collector/supplier
   * * stream: the count of a {@link PipelineCascadeNode#executeSupplierFromCollector()} execution driven by {@link Pipeline#getWindowInterval()}
   * * batch: 1
   */
  private final AtomicInteger executionCounter = new AtomicInteger(0);
  /**
   * The number of errors on the step
   */
  private final AtomicInteger errorCounter = new AtomicInteger(0);
  /**
   * The number of data resources parked
   */
  private final AtomicInteger parkingCounter = new AtomicInteger(0);

  private PipelineStepResult(PipelineStepResultBuilder pipelineStepResultBuilder) {
    this.builder = pipelineStepResultBuilder;
  }

  public static PipelineStepResultBuilder builder(PipelineStep pipelineStep) {
    return new PipelineStepResultBuilder(pipelineStep);
  }

  public PipelineStep getStep() {
    return this.builder.pipelineStep;
  }

  @Override
  public int compareTo(PipelineStepResult o) {
    return this.getStep().getPipelineStepId().compareTo(o.getStep().getPipelineStepId());
  }

  @Override
  public String toString() {
    return getStep().toString();
  }

  public long getInputCounter() {
    return inputCounter;
  }

  public long getOutputCounter() {
    return outputCounter;
  }

  /**
   * record a result
   */
  public void record(DataPath dataPath, PipelineStepResultDirection direction) {
    Objects.requireNonNull(dataPath);

    switch (direction) {
      case IN:
        inputCounter++;
        if (this.builder.collectDataPath) {
          this.inputDataPaths.add(dataPath);
        }
        break;
      case OUT:
        outputCounter++;
        if (this.builder.collectDataPath) {
          this.outputDataPaths.add(dataPath);
        }
        break;
      default:
        throw new InternalException(direction + " should have been processed");
    }
  }

  public Integer getExecutionCounter() {
    return this.executionCounter.get();
  }

  public void incrementExecutionCounter() {
    this.executionCounter.incrementAndGet();
  }

  public void incrementErrorCounter() {
    this.errorCounter.incrementAndGet();
  }

  public long getErrorCounter() {
    return this.errorCounter.get();
  }

  public void incrementParkingCounter() {
    this.parkingCounter.incrementAndGet();
  }

  public int getParkingCounter() {
    return this.parkingCounter.get();
  }

  public static class PipelineStepResultBuilder {
    private final PipelineStep pipelineStep;
    private boolean collectDataPath = false;

    public PipelineStepResultBuilder(PipelineStep pipelineStep) {
      this.pipelineStep = pipelineStep;
    }

    public PipelineStepResult build() {
      collectDataPath = this.pipelineStep.getPipeline().getMaxCycleCount() < 10;
      return new PipelineStepResult(this);
    }

  }
}
