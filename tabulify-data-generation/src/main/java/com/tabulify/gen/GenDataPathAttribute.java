package com.tabulify.gen;

import com.tabulify.conf.AttributeEnum;
import com.tabulify.spi.DataPath;
import net.bytle.type.KeyNormalizer;

public enum GenDataPathAttribute implements AttributeEnum {

  /**
   * The {@link DataPath#getAttribute(KeyNormalizer)} key giving the maximum number of rows generated
   * <p>
   * For now, if you want to move the maximum size higher,
   * you need to truncate the table, you can't just set it higher and rerun a data generation
   * <p>
   * TODO: To be able to move the max size up without truncating the table before,
   * this is possible to create a sequence generator for a
   * primary key but for a unique key on multiple columns
   * this is more difficult
   */
  MAX_RECORD_COUNT("The maximum of records generated", Long.class, 100L, true),
  STREAM_RECORD_COUNT("The records generated in a stream", Long.class, null, true),
  SIZE_NOT_CAPPED("The number of records without max", Long.class, null, false),
  SIZE("The size", Long.class, null, false);

  private final String description;
  private final Class<?> clazz;
  private final Object defaultValue;
  private final boolean isUpdatable;


  GenDataPathAttribute(String description, Class<?> valueClazz, Object defaultValue, boolean isUpdatable) {
    this.description = description;
    this.clazz = valueClazz;
    this.defaultValue = defaultValue;
    this.isUpdatable = isUpdatable;
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
  public boolean getIsUpdatable() {
    return this.isUpdatable;
  }

  @Override
  public Object getDefaultValue() {
    return this.defaultValue;
  }

}
