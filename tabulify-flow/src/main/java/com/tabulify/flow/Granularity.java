package com.tabulify.flow;


import com.tabulify.conf.AttributeValue;
import net.bytle.type.KeyNormalizer;

public enum Granularity implements AttributeValue {


  RECORD("Record granularity"),
  RESOURCE("Resource granularity");

  private final String description;


  Granularity( String description) {
    this.description = description;
  }


  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public String toString() {
    return KeyNormalizer.createSafe(this.name()).toCliLongOptionName();
  }

}
