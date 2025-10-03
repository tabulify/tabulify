package com.tabulify.service;

import com.tabulify.connection.ObjectOrigin;
import net.bytle.type.KeyNormalizer;

/**
 * All built-in system connection attribute
 */
public enum ServiceAttributeBase implements ServiceAttributeEnum {

  NAME("The name of the service", KeyNormalizer.class, null, false),
  TYPE("The type of the system", String.class, null, true),
  ORIGIN("The origin of the system", ObjectOrigin.class, null, false),
  IS_STARTED("The status of the service", Boolean.class, null, false);


  private final String description;
  private final Class<?> clazz;
  private final Object defaultValue;
  private final boolean isParameter;


  ServiceAttributeBase(String description, Class<?> valueClazz, Object defaultValue, boolean isParameter) {
    this.description = description;
    this.clazz = valueClazz;
    this.defaultValue = defaultValue;
    this.isParameter = isParameter;
  }

  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public Class<?> getValueClazz() {
    return this.clazz;
  }

  @Override
  public Object getDefaultValue() {
    return this.defaultValue;
  }

  @Override
  public boolean isParameter() {
    return this.isParameter;
  }

  @Override
  public Class<?> getCollectionElementClazz() {
    return null;
  }

  @Override
  public Class<?> getCollectionValueClazz() {
    return null;
  }
}
