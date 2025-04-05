package net.bytle.db.csv;

import net.bytle.type.Attribute;

public enum CsvDataPathAttribute implements Attribute {

  /**
   * The location (id) of the header row in the file (one being the first line)
   * <p>
   * Even without header, we create the structure (columns)
   * <p>
   * One because:
   * * this is the most common format found
   * * when we download data, the header is added
   */
  HEADER_ROW_ID("The id of the header row",Integer.class,1),
  /**
   * The third library use this character by default
   * even if not set
   */
  COMMENT_CHARACTER( "The comment character", Character.class, '#'),
  DELIMITER_CHARACTER("The delimiter character", Character.class, ','),
  ESCAPE_CHARACTER("The escape character", Character.class, null),

  QUOTE_CHARACTER("The quote character", Character.class, '"'),
  IGNORE_EMPTY_LINE( "Ignore empty line", Boolean.class, true);


  private final String desc;
  private final Class<?> clazz;
  private final Object defaultValue;

  CsvDataPathAttribute( String description, Class<?> clazz, Object defaultValue) {

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
