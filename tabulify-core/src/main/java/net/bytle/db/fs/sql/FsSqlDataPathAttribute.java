package net.bytle.db.fs.sql;

import net.bytle.type.Attribute;

public enum FsSqlDataPathAttribute implements Attribute {

  PARSING_MODE("How to parse the SQL files", FsSqlParsingModeValue.SQL, FsSqlParsingModeValue.class);

  private final String description;
  private final Object defaultValue;
  private final Class<?> clazz;

  FsSqlDataPathAttribute(String description, Object defaultValue, Class<?> clazz) {
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

  @Override
  public Object getDefaultValue() {
    return this.defaultValue;
  }
}
