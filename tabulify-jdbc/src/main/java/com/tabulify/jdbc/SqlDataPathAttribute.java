package com.tabulify.jdbc;

import net.bytle.type.Attribute;

public enum SqlDataPathAttribute implements Attribute {

  /**
   * For now, this attribute is not a global tabular attribute because data and class are not part of the tabular core module
   * (Solution: When creating the SqlConnection, we could add it)
   */
  QUERY_METADATA_DETECTION("How the metadata of a query are detected", SqlDataPathQueryMetadataDetectionMethod.class, SqlDataPathQueryMetadataDetectionMethod.TEMPORARY_VIEW),
  CATALOG("The catalog of the sql resource", String.class, null),
  SCHEMA("The schema of the sql resource", String.class, null),
  NAME("The name of the sql resource", String.class, null);

  private final String desc;
  private final Class<?> valueClazz;
  private final Object valueDefault;

  SqlDataPathAttribute(String description, Class<?> valueClazz, Object valueDefault) {
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
