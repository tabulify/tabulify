package com.tabulify.fs.textfile;

import net.bytle.type.Attribute;

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public enum FsTextDataPathAttributes implements Attribute {

  END_OF_RECORD("The End Of Record string sequence", DEFAULTS.EOLS, String[].class),
  CHARACTER_SET("The character set of the file", DEFAULTS.CHARSET, Charset.class),
  COLUMN_NAME("The name of the column when the text content is returned on one column", DEFAULTS.HEADER_DEFAULT, String.class);


  private final String description;
  private final Object defaultValue;
  private final Class<?> clazz;

  FsTextDataPathAttributes(String description, Object defaultValue, Class<?> clazz) {
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


  protected static class DEFAULTS {
    /**
     * By default, if not set, we follow the {@link BufferedReader#readLine()} definition of a line.
     * Ie a line ie a sequence of character terminated by any one of:
     * * a line feed ('\n'),
     * * a carriage return ('\r'),
     * * or a carriage return followed immediately by a linefeed.
     */
    public static final String[] EOLS = {"\n", "\r", "\r\n"};

    public static final String HEADER_DEFAULT = "lines";
    public static final Charset CHARSET = StandardCharsets.UTF_8;
  }

}
