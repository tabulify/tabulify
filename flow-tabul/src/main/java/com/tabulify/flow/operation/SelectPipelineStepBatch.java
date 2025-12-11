package com.tabulify.flow.operation;


import com.tabulify.flow.engine.PipelineStepRootBatchSupplierAbs;
import com.tabulify.spi.DataPath;

import java.util.ArrayDeque;
import java.util.List;


/**
 * A start operation in a stream
 * that generates a list of data path
 * This class does not care about {@link SelectPipelineStepArgumentOrder}
 * Each filter step:
 * * sort in function of its function.
 * * accumulate or not the data path
 * <p>
 * For instance, the drop step sorts in drop order, the list step orders in the order asked.
 */
public class SelectPipelineStepBatch extends PipelineStepRootBatchSupplierAbs {


  // fifo
  private final ArrayDeque<DataPath> dataPathQueue = new ArrayDeque<>();
  private final SelectPipelineStep selectBuilder;


  public SelectPipelineStepBatch(SelectPipelineStep selectBuilder) {
    super(selectBuilder);
    this.selectBuilder = selectBuilder;
  }


  @Override
  public DataPath get() {

    return dataPathQueue.poll();

  }

  @Override
  public void onStart() {

    List<DataPath> dataPaths = this.selectBuilder.produceDataPath();
    this.dataPathQueue.addAll(dataPaths);

  }


  @Override
  public boolean hasNext() {

    return !this.dataPathQueue.isEmpty();

  }


  @Override
  public PipelineStepProcessingType getProcessingType() {
    return this.selectBuilder.getProcessingType();
  }
}
