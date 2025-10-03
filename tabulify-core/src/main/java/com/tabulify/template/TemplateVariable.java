package com.tabulify.template;

import net.bytle.exception.CastException;
import net.bytle.type.KeyNormalizer;

import java.util.List;
import java.util.Set;

/**
 * A class that wraps a variable with a prefix
 */
public class TemplateVariable {
  private final KeyNormalizer templateVariable;
  private final KeyNormalizer prefix;
  private final KeyNormalizer variableWithoutPrefix;


  public TemplateVariable(String s, TemplateString.TemplateStringBuilder templateString) throws CastException {

    templateVariable = KeyNormalizer.create(s);
    List<String> parts = templateVariable.getNormalizedParts();
    switch (parts.size()) {
      case 0:
        throw new CastException("Template variable \"" + s + "\" has no parts");
      case 1:
        /**
         * Glob Back reference for instance ($0, $1, ...)
         */
        prefix = null;
        variableWithoutPrefix = templateVariable;
        break;
      default:
        KeyNormalizer tempPrefix;
        KeyNormalizer safe = KeyNormalizer.createSafe(parts.get(0));
        try {
          TemplatePrefix templatePrefix = TemplatePrefix.cast(safe);
          tempPrefix = templatePrefix.toKeyNormalizer();
        } catch (CastException e) {
          // not a known prefix
          Set<KeyNormalizer> extraPrefix = templateString.getExtraPrefixes();
          if (!extraPrefix.contains(safe)) {
            throw new CastException("The prefix (" + safe + ") in the variable (" + s + ") is unknown");
          }
          tempPrefix = safe;
        }
        prefix = tempPrefix;
        variableWithoutPrefix = KeyNormalizer.createSafe(parts.subList(1, parts.size()));
        break;
    }


  }

  public String getRawValue() {
    return templateVariable.toString();
  }

  public KeyNormalizer getPrefix() {
    return prefix;
  }

  public KeyNormalizer getVariableWithoutPrefix() {
    return variableWithoutPrefix;
  }

  @Override
  public String toString() {
    return templateVariable.toString();
  }
}
