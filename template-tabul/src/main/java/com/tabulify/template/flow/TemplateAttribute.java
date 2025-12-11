package com.tabulify.template.flow;

import com.tabulify.connection.ConnectionBuiltIn;
import com.tabulify.flow.Granularity;
import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.flow.operation.StepOutputArgument;
import com.tabulify.uri.DataUriStringNode;

import java.util.List;

import static com.tabulify.template.flow.TemplateTargetType.TEMPLATE_OUTPUT;

public enum TemplateAttribute implements ArgumentEnum {

  TEMPLATE_INLINE("Defining an inline template", null, null),
  TEMPLATE_INLINES("Defining several inline template", null, null),
  TEMPLATE_SELECTOR("Define one template selector", DataUriStringNode.class, null),
  TEMPLATE_SELECTORS("Define one or more template selector", List.class, null),
  TEMPLATE_ENGINE("The template engine", TemplateEngine.class, TemplateEngine.NATIVE),
  MODEL_VARIABLES("defined extra table variables", null, null),
  PROCESSING_TYPE("the processing type", ProcessingType.class, null),
  GRANULARITY("N to 1 or N to N template application (the default granularity is media type dependent)", Granularity.class, null),
  OUTPUT_TYPE("defined the type of output", StepOutputArgument.class, StepOutputArgument.TARGETS),
  TARGET_TYPE("defined the target type ", TemplateTargetType.class, TEMPLATE_OUTPUT),
  TARGET_COLUMN_NAME("the name of the target column in a template format", String.class, "${template_media_subtype}"),
  TEMPLATE_EMAIL("if true, the css for html template will be inlined (used for email template)", Boolean.class, false),
  TARGET_DATA_URI("A template data uri that defines the data uri of the templating results", DataUriStringNode.class, DataUriStringNode.createFromStringSafe("${input_logical_name}_${template_name}@" + ConnectionBuiltIn.MEMORY_CONNECTION)),
  ;

  private final String description;
  private final Class<?> clazz;
  private final Object defaultValue;

  TemplateAttribute(String description, Class<?> clazz, Object defaultValue) {

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
