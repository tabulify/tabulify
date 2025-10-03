package com.tabulify.template;

import com.tabulify.conf.Attribute;
import com.tabulify.spi.Meta;
import com.tabulify.spi.StrictException;
import net.bytle.exception.CastException;
import net.bytle.template.TextTemplate;
import net.bytle.template.TextTemplateEngine;
import net.bytle.type.KeyNormalizer;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;

/**
 * A utility class that calculate a path from a template
 */
public class TemplateString {


  private final TemplateStringBuilder templateStringBuilder;


  public TemplateString(TemplateStringBuilder templateStringBuilder) {
    this.templateStringBuilder = templateStringBuilder;
  }

  public static TemplateStringBuilder builder(String template) {
    return new TemplateStringBuilder(template);
  }


  /**
   * @param templateMetas - A variables builder so that we get also the context
   * @return the name
   * @throws StrictException if a value was not found for a variable and that the run is strict
   */
  public String apply(TemplateMetas templateMetas) {

    if (this.templateStringBuilder.pipeline != null) {
      templateMetas.addMeta(this.templateStringBuilder.pipeline, TemplatePrefix.PIPELINE);
    }

    /**
     * The map of variable name and its value
     */
    Map<String, Object> variableValueMap = new HashMap<>();

    /**
     * Search the value case independent
     */
    List<TemplateVariable> variableNames = this.templateStringBuilder.variableNames;
    for (TemplateVariable templateVariable : variableNames) {

      Object valueOrDefault = this.getPublicValue(templateVariable, templateMetas);
      if (valueOrDefault == null) {
        if (this.templateStringBuilder.isStrict) {
          StringBuilder context = new StringBuilder();
          for (Map.Entry<KeyNormalizer, Meta> entry : templateMetas.getVariablePrefixMetaMap().entrySet()) {
            context.append("  - Prefix: ").append(entry.getKey().toCliLongOptionName()).append(", Meta Type: ").append(entry.getValue().getClass().getSimpleName()).append(", Meta Name: ").append(entry.getValue()).append(System.lineSeparator());
          }
          String errorMessage = "We couldn't calculate the template string.\n" +
            "The variable (" + templateVariable + ") in the template string (" + this.templateStringBuilder.templateString + ") was not found. \nAvailable Metas: \n" + context;
          throw new StrictException(errorMessage);
        }
        continue;
      }
      variableValueMap.put(templateVariable.getRawValue(), toFormattedString(valueOrDefault));

    }

    return this.templateStringBuilder.textTemplateEngine.applyVariables(variableValueMap).getResult();

  }

  private Object getPublicValue(TemplateVariable templateVariable, TemplateMetas templateMetas) {


    Attribute attribute = templateMetas.getAttribute(templateVariable);
    if (attribute == null) {
      return null;
    }
    Object valueOrDefault = attribute.getPublicValueOrProvider();
    if (valueOrDefault == null) {
      return null;
    }
    if (valueOrDefault instanceof Supplier) {
      valueOrDefault = ((Supplier<?>) valueOrDefault).get();
    }
    return valueOrDefault;


  }


  private Object toFormattedString(Object value) {
    if (value instanceof Timestamp) {
      Timestamp timestamp = ((Timestamp) value);
      return timestamp.toLocalDateTime().format(this.templateStringBuilder.dateTimeFormatter);
    }
    return value;
  }

  public List<TemplateVariable> getVariables() {
    return this.templateStringBuilder.variableNames;
  }

  public static class TemplateStringBuilder {


    private final TextTemplate textTemplateEngine;
    private final List<TemplateVariable> variableNames = new ArrayList<>();
    private final String templateString;
    public Map<KeyNormalizer, Object> variableValueMap = new HashMap<>();
    private boolean isStrict = true;
    // Date with Best format for cross-platform compatibility
    private DateTimeFormatter dateTimeFormatter;
    //private Pipeline pipeline;

    /**
     * Extra template prefix
     * For parsing, we need to know the prefix
     */
    private final Set<KeyNormalizer> templatePrefixes = new HashSet<>();
    private Meta pipeline;


    public TemplateStringBuilder(String template) {
      this.templateString = template;
      textTemplateEngine = TextTemplateEngine.getOrCreate().compile(template);
    }

    public TemplateString build() {

      long dollarCount = templateString.chars().filter(ch -> ch == '$').count();
      if (dollarCount != textTemplateEngine.getVariableNames().size()) {
        long badWrittenVariables = dollarCount - textTemplateEngine.getVariableNames().size();
        throw new IllegalArgumentException("The target template contains " + badWrittenVariables + " bad written variables (" + templateString + "). Template variable names should have only letters, digits and underscore");
      }
      if (dateTimeFormatter == null) {
        dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
      }
      /**
       * Must come after the {@link #templatePrefixes} have been set
       */
      for (String variableString : textTemplateEngine.getVariableNames()) {
        try {
          variableNames.add(new TemplateVariable(variableString, this));
        } catch (CastException e) {
          throw new IllegalArgumentException("The template variable (" + variableString + ") in the template string (" + this.templateString + ") is not valid. Error: " + e.getMessage(), e);
        }
      }
      return new TemplateString(this);
    }

    public TemplateStringBuilder isStrict(boolean strict) {
      isStrict = strict;
      return this;
    }

    public TemplateStringBuilder setDateTimeFormat(String dateTimeFormat) {
      this.dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimeFormat);
      return this;
    }

    /**
     * @param values A map of variable name values
     */
    public TemplateStringBuilder setVariableValueMap(Map<KeyNormalizer, Object> values) {
      this.variableValueMap = values;
      return this;
    }

    public TemplateStringBuilder setPipeline(Meta pipeline) {
      this.pipeline = pipeline;
      return this;
    }

    /**
     * @param templatePrefix - extra template prefix set by the step (extra {@link TemplatePrefix})
     */
    public TemplateStringBuilder setExtraPrefixes(KeyNormalizer templatePrefix) {
      this.templatePrefixes.add(templatePrefix);
      return this;
    }

    /**
     * @param templatePrefixes - extra template prefix set by the step (extra {@link TemplatePrefix})
     */
    public TemplateStringBuilder setExtraPrefixes(Set<KeyNormalizer> templatePrefixes) {
      this.templatePrefixes.addAll(templatePrefixes);
      return this;
    }

    public Set<KeyNormalizer> getExtraPrefixes() {
      return this.templatePrefixes;
    }
  }
}
