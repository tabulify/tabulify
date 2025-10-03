package com.tabulify.flow.engine;

import com.tabulify.conf.AttributeValue;
import net.bytle.exception.CastException;
import net.bytle.type.Casts;
import net.bytle.type.KeyNormalizer;

/**
 * Common attributes of an operation
 *
 */
public enum PipelineStepAttribute implements AttributeValue {

  NAME("The name of the step"),
  OPERATION("The operation name"),
  ARGUMENTS("The operation args"),
  // comment and not description because this is the name used in a relational database
  COMMENT("A step description"),
  ;

  private final String comment;

  PipelineStepAttribute(String description) {

    this.comment = description;

  }

  public static PipelineStepAttribute cast(String key) throws CastException {
    KeyNormalizer keyNormalized = KeyNormalizer.createSafe(key);
    if (keyNormalized.equals(KeyNormalizer.createSafe("args"))) {
      return PipelineStepAttribute.ARGUMENTS;
    }
    if (keyNormalized.equals(KeyNormalizer.createSafe("op"))) {
      return PipelineStepAttribute.OPERATION;
    }

    return Casts.cast(keyNormalized, PipelineStepAttribute.class);


  }

  @Override
  public String getDescription() {
    return this.comment;
  }

  @Override
  public String toString() {
    return KeyNormalizer.createSafe(name()).toCliLongOptionName();
  }
}
