package com.tabulify.spi;

import net.bytle.type.Attribute;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public enum DataPathAttribute implements Attribute {

  /**
   * Internal Properties Key
   */
  NAME("The name of the data resource",String.class),
  LOGICAL_NAME("The logical name", String.class),
  PARENT("The parent", String.class),
  CONNECTION("The connection name", String.class),
  DATA_URI("The data uri", String.class),
  PATH("The relative path to the default connection path", String.class),
  ABSOLUTE_PATH("The absolute path on the data system", String.class),
  COUNT("The number of records", String.class),
  SIZE("The number of byte", Integer.class),
  TYPE("The media type", String.class),
  SUBTYPE("The media subType", String.class),
  MD5("The Md5 hash", String.class),
  SHA384("The Sha384 hash", String.class),
  SHA384_INTEGRITY("The sha384 value used in the html integrity attribute", String.class),
  COLUMNS("The columns definition", String.class),
  PRIMARY_COLUMNS("The primary columns definition", String.class);


  /**
   * The key value that comes in the info output report
   */
  private final String description;
  private final Class<?> clazz;


  /**
   */
  DataPathAttribute(String description, Class<?> clazz) {

    this.description = description;
    this.clazz = clazz;
  }

  public static Set<DataPathAttribute> getScalarAttributes() {
    return Arrays.stream(values())
      .filter(c -> !Objects.equals(COLUMNS, c))
      .collect(Collectors.toSet());
  }


  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public Class<?> getValueClazz() {
    return clazz;
  }

  @Override
  public Object getDefaultValue() {
    return null;
  }


  @Override
  public String toString() {
    return super.toString().toLowerCase();
  }

}
