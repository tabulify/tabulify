package com.tabulify.excel;

import com.tabulify.conf.AttributeEnum;

public enum ExcelDataPathAttribute implements AttributeEnum {


  // 1 because it's more human
  HEADER_ROW_ID("The row id of the header row - (0 means no header, 1 the first line) ", Integer.class, 1),

  SHEET_NAME( "The name of the sheet (By default, the first one)", String.class, null),

  // Default format String
  // See Format Cells > Number > Custom
  TIMESTAMP_FORMAT("The default timestamp format string (Format Cells > Number > Custom)", String.class, Constants.DEFAULT_TIMESTAMP_FORMAT),
  DATE_FORMAT("The default date format string (Format Cells > Number > Custom)", String.class, Constants.DEFAULT_DATE_FORMAT);


  private final String desc;
  private final Class<?> clazz;
  private final Object defaultValue;

  ExcelDataPathAttribute(String description, Class<?> clazz, Object defaultValue) {

    this.desc = description;
    this.clazz = clazz;
    this.defaultValue = defaultValue;
  }



  @Override
  public String getDescription() {
    return this.desc;
  }

  @Override
  public Class<?> getValueClazz() {
    return this.clazz;
  }

  @Override
  public Object getDefaultValue() {
    return this.defaultValue;
  }

  public static class Constants {
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static final String DEFAULT_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";
  }

}
