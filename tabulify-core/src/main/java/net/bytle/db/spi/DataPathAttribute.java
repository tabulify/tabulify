package net.bytle.db.spi;

import net.bytle.type.Attribute;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public enum DataPathAttribute implements Attribute {

  /**
   * Internal Properties Key
   */
  NAME("The name of the data resource"),
  LOGICAL_NAME("The logical name"),
  PARENT("The parent"),
  CONNECTION("The connection name"),
  DATA_URI("The data uri"),
  PATH("The relative path to the default connection path"),
  ABSOLUTE_PATH("The absolute path on the data system"),
  COUNT("The number of records"),
  SIZE("The number of byte"),
  TYPE("The media type"),
  SUBTYPE("The media subType"),
  MD5("The Md5 hash"),
  SHA384("The Sha384 hash"),
  SHA384_INTEGRITY("The sha384 value used in the html integrity attribute"),
  COLUMNS("The columns definition"),
  PRIMARY_COLUMNS("The primary columns definition");


  /**
   * The key value that comes in the info output report
   */
  private final String description;


  /**
   */
  DataPathAttribute(String description) {

    this.description = description;
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
    return null;
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
