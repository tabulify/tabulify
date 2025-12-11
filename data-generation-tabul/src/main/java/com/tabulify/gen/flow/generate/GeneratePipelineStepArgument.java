package com.tabulify.gen.flow.generate;

import com.tabulify.flow.Granularity;
import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.flow.operation.PipelineStepProcessingType;
import com.tabulify.type.KeyNormalizer;

import java.util.List;

public enum GeneratePipelineStepArgument implements ArgumentEnum {


  DATA_SELECTOR("A data selector to select generators", null, String.class),
  DATA_SELECTORS("A list of data selector to select generators", null, List.class),
  STRICT_SELECTION("If true, fail if the selection does not return any generators", true, Boolean.class),
  PROCESSING_TYPE("The type of processing", PipelineStepProcessingType.STREAM, PipelineStepProcessingType.class),
  STREAM_RECORD_COUNT("The default number of record generated in stream (if not set in the generator)", 10L, Long.class),
  STREAM_GRANULARITY("The stream granularity (ie record or resource)", Granularity.RECORD, Granularity.class);


  private final Object defaultValue;
  private final String description;
  private final Class<?> clazz;

  GeneratePipelineStepArgument(String description, Object defaultValue, Class<?> clazz) {

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

  public Object getDefaultValue() {
    return this.defaultValue;
  }

  @Override
  public String toString() {
    return KeyNormalizer.createSafe(name()).toCliLongOptionName();
  }

}
