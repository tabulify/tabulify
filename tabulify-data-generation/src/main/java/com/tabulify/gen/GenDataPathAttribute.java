package com.tabulify.gen;

import com.tabulify.spi.DataPath;
import net.bytle.type.Attribute;

public enum GenDataPathAttribute implements Attribute {

  /**
   * The {@link DataPath#getVariable(String)} key giving the maximum number of rows generated
   * <p>
   * For now, if you want to move the maximum size higher,
   * you need to truncate the table, you can't just set it higher and rerun a data generation
   * <p>
   * TODO: To be able to move the max size up without truncating the table before,
   * this is possible to create a sequence generator for a
   * primary key but for a unique key on multiple columns
   * this is more difficult
   */
  MAX_RECORD_COUNT_PROPERTY_KEY("The origin of the connection", Long.class, 100L);

  private final String description;
  private final Class<?> clazz;
  private final Object defaultValue;


  GenDataPathAttribute(String description, Class<?> valueClazz, Object defaultValue) {
    this.description = description;
    this.clazz = valueClazz;
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
