package com.tabulify.flow.engine;

import com.tabulify.conf.AttributeEnum;
import net.bytle.exception.CastException;
import net.bytle.type.Casts;
import net.bytle.type.KeyNormalizer;

import java.util.List;

public enum PipelineFileAttribute implements AttributeEnum {

  STEPS("The Pipeline steps", List.class),
  ARGUMENTS("The pipeline arguments", List.class),
  COMMENT("A comment", String.class),
  ;

  private final String description;
  private final Class<?> clazz;
  private static final KeyNormalizer ARGS = KeyNormalizer.createSafe("args");


  PipelineFileAttribute(String description, Class<?> clazz) {
    this.description = description;
    this.clazz = clazz;
  }


  public static PipelineFileAttribute cast(KeyNormalizer keyNormalizer) throws CastException {
    if (keyNormalizer.equals(ARGS)) {
      return ARGUMENTS;
    }
    return Casts.cast(keyNormalizer, PipelineFileAttribute.class);
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

}
