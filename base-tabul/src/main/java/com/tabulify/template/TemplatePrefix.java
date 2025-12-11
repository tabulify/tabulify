package com.tabulify.template;

import com.tabulify.exception.CastException;
import com.tabulify.type.Casts;
import com.tabulify.type.KeyNormalizer;

public enum TemplatePrefix {

  INPUT("in"),
  EXECUTABLE("exe"),
  TARGET("tar"),
  PIPELINE("pipe"),
  RECORD("rec");

  private final KeyNormalizer keyNormalizer;
  private final KeyNormalizer shortName;

  TemplatePrefix(String shortPrefix) {
    this.shortName = KeyNormalizer.createSafe(shortPrefix);
    this.keyNormalizer = KeyNormalizer.createSafe(this.name());
  }

  static TemplatePrefix cast(KeyNormalizer o) throws CastException {

    for (TemplatePrefix templatePrefix : TemplatePrefix.values()) {
      if (o.equals(templatePrefix.getShort())) {
        return templatePrefix;
      }
    }
    return Casts.cast(o, TemplatePrefix.class);
  }

  public static KeyNormalizer normalization(KeyNormalizer prefix) {
    // Normalization
    try {
      return TemplatePrefix.cast(prefix).toKeyNormalizer();
    } catch (CastException e) {
      // not a known prefix
      return prefix;
    }
  }

  KeyNormalizer toKeyNormalizer() {
    return this.keyNormalizer;
  }

  KeyNormalizer getShort() {
    return this.shortName;
  }
}
