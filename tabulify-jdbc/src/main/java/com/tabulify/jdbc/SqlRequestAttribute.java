package com.tabulify.jdbc;

import com.tabulify.conf.AttributeEnum;

import java.util.List;

import static com.tabulify.jdbc.SqlQueryMetadataDetectionMethod.*;

public enum SqlRequestAttribute implements AttributeEnum {

  PARAMETERS("A list of parameters for prepared or callable statements", List.class, List.of()),
  /**
   * Strict execution default value is updated based on the tabular
   * value
   */
  STRICT_EXECUTION("Stop the execution at the first error", Boolean.class, true),
  SELECT_METADATA_DETECTIONS("How the metadata of a SQL SELECT are detected", List.class, List.of(DESCRIBE, TEMPORARY_VIEW, FALSE_EQUALITY)),
  ;

  private final String desc;
  private final Class<?> valueClazz;
  private final Object valueDefault;

  SqlRequestAttribute(String description, Class<?> valueClazz, Object valueDefault) {
    this.desc = description;
    this.valueClazz = valueClazz;
    this.valueDefault = valueDefault;
  }

  @Override
  public String getDescription() {
    return this.desc;
  }

  @Override
  public Class<?> getValueClazz() {
    return this.valueClazz;
  }

  @Override
  public Object getDefaultValue() {
    return this.valueDefault;
  }

}
