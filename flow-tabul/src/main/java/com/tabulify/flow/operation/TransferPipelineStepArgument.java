package com.tabulify.flow.operation;

import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.transfer.TransferMappingMethod;
import com.tabulify.transfer.TransferOperation;
import com.tabulify.transfer.TransferPropertiesCross;
import com.tabulify.transfer.UpsertType;
import com.tabulify.type.KeyNormalizer;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum TransferPipelineStepArgument implements ArgumentEnum {

  OUTPUT_TYPE("The resource that is passed as output", StepOutputArgument.TARGETS, StepOutputArgument.class),
  TRANSFER_OPERATION("defines the transfer operation (" + Arrays.stream(TransferOperation.values()).map(TransferOperation::toString).collect(Collectors.joining(", ")) + "). ", null, TransferOperation.class),
  TRANSFER_UPSERT_TYPE("defines the type of upsert operation (" + Arrays.stream(UpsertType.values()).map(UpsertType::toString).collect(Collectors.joining(", ")) + "). ", null, UpsertType.class),
  TRANSFER_MAPPING_METHOD("defines the method used to map the source columns to the target columns", null, TransferMappingMethod.class),
  TRANSFER_MAPPING_STRICT("defines if a map by name or position is strict", null, Boolean.class),
  TRANSFER_MAPPING_COLUMNS("defines the columns mapping between the source and the target", null, Map.class),
  TARGET_OPERATION("defines the data operations (drop or truncate) on the existing target before transfer. A `replace` operation will drop the target.", null, String.class),
  SOURCE_OPERATION("defines the data operation (drop or truncate) on the source after transfer. Note: A `move` operation will drop the source.", null, String.class),
  TARGET_COMMIT_FREQUENCY("defines the commit frequency in number of batches against the target data resource", TransferPropertiesCross.DEFAULT_COMMIT_FREQUENCY, Integer.class),
  TARGET_BATCH_SIZE("defines the batch size against the target data resource", TransferPropertiesCross.DEFAULT_BATCH_SIZE, Integer.class),
  WITH_PARAMETERS("defines if parameters are used in the SQL statement", true, Boolean.class),
  METRICS_DATA_URI("defines a target data uri where the data metrics should be exported", null, String.class),
  SOURCE_FETCH_SIZE("defines the size of the network message from the source to fetch the data", TransferPropertiesCross.DEFAULT_FETCH_SIZE, Integer.class),
  // No default because the buffer size is dependent on the fetch size,
  BUFFER_SIZE("defines the size of the memory buffer between the source and target threads", null, Integer.class),
  TARGET_WORKER_COUNT("defines the target number of thread against the target connection", TransferPropertiesCross.DEFAULT_TARGET_WORKER_COUNT, Integer.class),
  PROCESSING_TYPE("how to process the inputs (one by one or in batch)", PipelineStepProcessingType.BATCH, PipelineStepProcessingType.class);


  private final String description;
  private final Object defaultValue;
  private final Class<?> clazz;

  TransferPipelineStepArgument(String description, Object defaultValue, Class<?> clazz) {

    this.description = description;
    this.defaultValue = defaultValue;
    this.clazz = clazz;

  }


  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public Class<?> getValueClazz() {
    return this.clazz;
  }


  /**
   * @return The default value if not defined
   */
  public Object getDefaultValue() {
    return defaultValue;
  }

  @Override
  public String toString() {
    return KeyNormalizer.createSafe(name()).toCliLongOptionName();
  }


}
