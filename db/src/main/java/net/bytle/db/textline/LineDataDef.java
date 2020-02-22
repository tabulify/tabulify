package net.bytle.db.textline;


import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.TableDef;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Implementation of text line format
 * A data path with a line tabular structure (one cell by row = one line)
 *
 *
 */
public class LineDataDef extends TableDef {

  /**
   * See {@link #setCharset(Charset)}
   */
  private Charset charset = StandardCharsets.UTF_8;

  /**
   * The path of the file
   */
  private final LineDataPath lineDataPath;

  /**
   * See {@link #setNewLineCharacters(String)}
   */
  private String newLineCharacters = System.lineSeparator();

  public String getNewLineCharacters() {
    return newLineCharacters;
  }

  /**
   * Set the newline string
   *
   * @param newLineCharacters The strings that is used at the end of a row (default to the system default \r\n for Windows, \n for the other)
   */
  public LineDataDef setNewLineCharacters(String newLineCharacters) {
    this.newLineCharacters = newLineCharacters;
    return this;
  }


  /**
   * Set the character set of the file
   *
   * @param charset The character encoding for the file - Default: UTf-8
   * @return The {@link LineDataDef CsvDataDef} instance for chaining initialization
   */
  public LineDataDef setCharset(Charset charset) {
    this.charset = charset;
    return this;
  }

  /**
   * @param dataPath The CsvDataPath
   */
  public LineDataDef(LineDataPath dataPath) {
    super(dataPath);
    this.lineDataPath = dataPath;
  }


  @Override
  public LineDataPath getDataPath() {
    return lineDataPath;
  }



  /**
   * @return the {@link #charset}
   */
  public Charset getCharset() {
    return charset;
  }

  @Override
  public <T> ColumnDef<T> getColumnOf(String columnName, Class<T> clazz) {
    if (super.getColumnsSize()>=1) {
      throw new RuntimeException("The text format has only on column which it's a line, you can't add a column");
    }
    return super.getColumnOf(columnName, clazz);
  }
}
