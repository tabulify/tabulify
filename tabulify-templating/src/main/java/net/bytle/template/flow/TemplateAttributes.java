package net.bytle.template.flow;

import com.tabulify.flow.Granularity;
import com.tabulify.conf.AttributeEnum;

import static net.bytle.template.flow.TemplateOutputOperation.EXTENDED_RECORDS;

public enum TemplateAttributes implements AttributeEnum {

  TEMPLATE_INLINE("Defining an inline template", null, null),
  TEMPLATE_INLINES("Defining several inline template", null, null),
  TEMPLATE_SELECTOR("Define one template selector", String.class, null),
  TEMPLATE_SELECTORS("Define one or more template selector", null, null),
  TEMPLATE_ENGINE("The template engine", TemplateEngine.class, TemplateEngine.NATIVE),
  TEMPLATE_TABULAR_VARIABLES("defined extra table variables", null, null),
  STEP_GRANULARITY("N to 1 or N to N template application (the default granularity is media type dependent)", Granularity.class, null),
  STEP_OUTPUT("defined the type of output", TemplateOutputOperation.class, EXTENDED_RECORDS),
  STEP_OUTPUT_LOGICAL_NAME("a pattern that defines the logical name of the output", String.class, null),
  TEMPLATE_EMAIL("if true, the css for html template will be inlined (used for email template)", Boolean.class, false);

  private final String description;
  private final Class<?> clazz;
  private final Object defaultValue;

  TemplateAttributes(String description, Class<?> clazz, Object defaultValue) {

    this.description = description;
    this.clazz = clazz;
    this.defaultValue = defaultValue;

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

}

