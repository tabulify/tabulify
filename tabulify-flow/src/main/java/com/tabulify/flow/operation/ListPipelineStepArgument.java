package com.tabulify.flow.operation;


import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.spi.DataPathAttribute;
import net.bytle.type.KeyNormalizer;

import java.util.List;

public enum ListPipelineStepArgument implements ArgumentEnum {


  /**
   * String because we don't know all data resources attributes
   * An implementation may add one
   */
  ATTRIBUTES("The data resources attributes to return", List.of(DataPathAttribute.NAME.toString(), DataPathAttribute.MEDIA_TYPE.toString()), List.class)
  ;

  private final Object defaultValue;
  private final String description;
  private final Class<?> clazz;

  ListPipelineStepArgument(String description, Object defaultValue, Class<?> clazz) {

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
