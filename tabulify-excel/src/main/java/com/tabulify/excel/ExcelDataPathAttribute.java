package com.tabulify.excel;

import net.bytle.type.Attribute;

public enum ExcelDataPathAttribute implements Attribute {


  HEADER_ROW_ID("The row id of the header row - Only 0 (no header) or 1 are supported",Integer.class,0),

  SHEET_NAME( "The name of the sheet (By default, the first one)", String.class, null);



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

}
