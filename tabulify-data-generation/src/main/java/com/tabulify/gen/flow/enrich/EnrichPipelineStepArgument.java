package com.tabulify.gen.flow.enrich;

import com.tabulify.flow.engine.ArgumentEnum;
import net.bytle.type.KeyNormalizer;

import java.util.Map;

public enum EnrichPipelineStepArgument implements ArgumentEnum {

  DATA_DEF("The virtual columns in a data definitions format", Map.class);


  private final String description;
  private final Class<?> clazz;


  EnrichPipelineStepArgument(String description, Class<?> aClass) {

    this.description = description;
    this.clazz = aClass;

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
    return null;
  }

  @Override
  public String toString() {
    return KeyNormalizer.createSafe(name()).toCliLongOptionName();
  }

}
