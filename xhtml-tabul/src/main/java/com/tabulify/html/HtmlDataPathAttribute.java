package com.tabulify.html;

import com.tabulify.conf.AttributeEnum;

public enum HtmlDataPathAttribute implements AttributeEnum {


  TABLE_SELECTOR("The css selector of the context node (by default table)","table"),
  HEADER_SELECTOR( "The css selector of the header node (by default th - table header )","th"),
  ROW_SELECTOR( "The css selector of the row nodes (by default tr - table row)","tr"),
  CELL_SELECTOR( "The css selector of the data nodes (by default td - table data)","thd,td")
  ;


  private final String desc;
  private final Object defaultValue;

  HtmlDataPathAttribute(String description, Object defaultValue) {

    this.desc = description;
    this.defaultValue = defaultValue;

  }


  @Override
  public String getDescription() {
    return this.desc;
  }

  @Override
  public Class<?> getValueClazz() {
    return String.class;
  }

  @Override
  public Object getDefaultValue() {
    return this.defaultValue;
  }

}
