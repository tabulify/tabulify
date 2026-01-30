package com.tabulify.template;

import com.tabulify.exception.CastException;
import com.tabulify.type.KeyNormalizer;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A class that wraps a variable with a prefix
 */
public class TemplateVariable {
    private final KeyNormalizer templateVariable;
    private final KeyNormalizer prefix;
    private final KeyNormalizer variableWithoutPrefix;


    public TemplateVariable(String variableName, TemplateString.TemplateStringBuilder templateString) throws CastException {

        templateVariable = KeyNormalizer.create(variableName);
        List<String> parts = templateVariable.getNormalizedParts();
        switch (parts.size()) {
            case 0:
                throw new CastException("Template variable \"" + variableName + "\" has no parts");
            case 1:
                /**
                 * Glob Back reference for instance ($0, $1, ...)
                 */
                prefix = null;
                variableWithoutPrefix = templateVariable;
                break;
            default:
                KeyNormalizer tempPrefix;
                KeyNormalizer prefix = KeyNormalizer.createSafe(parts.get(0));
                try {
                    TemplatePrefix templatePrefix = TemplatePrefix.cast(prefix);
                    tempPrefix = templatePrefix.toKeyNormalizer();
                } catch (CastException e) {
                    // not a known prefix
                    Set<KeyNormalizer> extraPrefix = templateString.getExtraPrefixes();
                    if (!extraPrefix.contains(prefix)) {
                        Set<KeyNormalizer> allPrefixes = Arrays.stream(TemplatePrefix.class.getEnumConstants())
                                .map(Object::toString)
                                .map(KeyNormalizer::createSafe)
                                .collect(Collectors.toSet());
                        allPrefixes.addAll(extraPrefix);
                        throw new CastException("The prefix (" + prefix + ") in the variable (" + variableName + ") is unknown. Possible prefix values: " + allPrefixes.stream().map(KeyNormalizer::toCliLongOptionName).collect(Collectors.joining(", ")));
                    }
                    tempPrefix = prefix;
                }
                this.prefix = tempPrefix;
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
