package com.tabulify.flow.operation;

import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.uri.DataUriStringNode;
import com.tabulify.type.KeyNormalizer;

import java.util.List;
import java.util.Map;

import static com.tabulify.flow.operation.SelectPipelineStepArgumentOrder.NATURAL;

public enum SelectPipelineStepArgument implements ArgumentEnum {


  DATA_SELECTOR("A selector", null, DataUriStringNode.class),
  DATA_SELECTORS("Multiple selectors", null, List.class),
  WITH_DEPENDENCIES("Add the dependencies to the selection", false, Boolean.class),
  STRICT_SELECTION("Fail if no data resources are selected when strict", false, Boolean.class),
  MEDIA_TYPE("The media type of the selected resources", null, String.class),
  LOGICAL_NAME("The logical name in a pattern format", null, String.class),
  DATA_ATTRIBUTES("The data attributes defined in data definition format", null, Map.class),
  ORDER("The order of the data resources", NATURAL, SelectPipelineStepArgumentOrder.class),
  PROCESSING_TYPE("Processing Type", PipelineStepProcessingType.BATCH, PipelineStepProcessingType.class);



  private final Object defaultValue;
  private final String description;
  private final Class<?> clazz;

  SelectPipelineStepArgument(String description, Object defaultValue, Class<?> clazz) {

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
