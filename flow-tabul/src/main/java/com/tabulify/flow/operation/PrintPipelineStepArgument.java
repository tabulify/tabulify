package com.tabulify.flow.operation;


import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.stream.PrinterPrintFormat;
import com.tabulify.type.KeyNormalizer;

public enum PrintPipelineStepArgument implements ArgumentEnum {


  /**
   * Null because the format may be determined by the type of resource
   */
  FORMAT("The predefined format to return", null, PrinterPrintFormat.class),
  PRINT_NON_VISIBLE_CHARACTERS("Print the non-visible character", true, Boolean.class),
  PRINT_COLUMN_HEADERS("Print the column headers", true, Boolean.class),
  PRINT_TABLE_HEADER("Print the table header (resource name and comment)", true, Boolean.class),
  FOOTER_SEPARATION_LINE_COUNT("The number of empty lines printed as footer", 1, Integer.class),
  COLORS_COLUMN_NAME("A column that store the colors definition", null, String.class),
  BOOLEAN_TRUE_TOKEN("The token shown when a value is true", "✓", String.class),
  BOOLEAN_FALSE_TOKEN("The token shown when a value is false", "", String.class),
  NULL_TOKEN("The token shown when a value is null", "<null>", String.class),
  STRING_EMPTY_TOKEN("The token shown when a string is empty", "<empty>", String.class),
  STRING_BLANK_TOKEN("The token shown when a string is blank", "<blank>", String.class),
  ;

  private final Object defaultValue;
  private final String description;
  private final Class<?> clazz;

  PrintPipelineStepArgument(String description, Object defaultValue, Class<?> clazz) {

    this.description = description;
    this.defaultValue = defaultValue;
    this.clazz = clazz;

  }

  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public Class<?> getValueClazz() {
    return this.clazz;
  }

  public Object getDefaultValue() {
    return this.defaultValue;
  }

  @Override
  public String toString() {
    return KeyNormalizer.createSafe(name()).toCliLongOptionName();
  }

}
