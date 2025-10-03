package com.tabulify.diff;

import com.tabulify.conf.AttributeEnum;
import net.bytle.type.KeyNormalizer;


public enum DataDiffColumn implements AttributeEnum {

  /**
   * The only mandatory column
   */
  STATUS("The change symbol and location of the change", String.class),
  /**
   * Optional columns
   */
  COLORS("Colors scheme to apply for print highlighting (ie ([1-9]*(r|g|b){1})(,){0,1})*)", String.class),
  ID("The record id of the comparison report", Integer.class),
  CHANGE_ID("The id of the change (ie change counter)", Integer.class),
  ORIGIN_ID("The natural id of the origin (source/target) if an unique/drive column was not defined", Integer.class),
  ORIGIN("The origin of the record (source or target)", String.class);


  private final String comment;
  private final Class<?> valueClazz;


  DataDiffColumn(String comment, Class<?> aClass) {

    this.comment = comment;
    this.valueClazz = aClass;
  }


  @Override
  public String getDescription() {
    return this.comment;
  }

  @Override
  public Class<?> getValueClazz() {
    return this.valueClazz;
  }

  @Override
  public Object getDefaultValue() {
    return null;
  }

  public KeyNormalizer toKeyNormalizer() {
    return KeyNormalizer.createSafe(this.name());
  }

}
