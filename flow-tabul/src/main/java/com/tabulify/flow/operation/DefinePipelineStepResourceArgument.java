package com.tabulify.flow.operation;

import com.tabulify.conf.AttributeEnum;
import com.tabulify.type.KeyNormalizer;
import com.tabulify.uri.DataUriStringNode;

import java.util.List;
import java.util.Map;

public enum DefinePipelineStepResourceArgument implements AttributeEnum {

  // One via Data Uri
  DATA_URI("Data Uri", DataUriStringNode.class),
  MEDIA_TYPE("The media type of the resource", String.class),
  // One via Inline
  DATA_RECORDS("Data records (as list of array)", List.class),
  DATA_DEF("Data attributes in a data definition format", Map.class);

  private final String description;
  private final Class<?> clazz;


  DefinePipelineStepResourceArgument(String description, Class<?> clazz) {

    this.description = description;
    this.clazz = clazz;

  }


  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public Class<?> getValueClazz() {
    return clazz;
  }

  @Override
  public Object getDefaultValue() {
    return null;
  }

  @Override
  public String toString() {
    return KeyNormalizer.createSafe(name()).toCliLongOptionName();
  }

}
