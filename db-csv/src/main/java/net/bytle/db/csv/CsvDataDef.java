package net.bytle.db.csv;


import net.bytle.db.fs.FsDataPath;
import net.bytle.db.fs.FsTableSystemLog;
import net.bytle.db.textline.LineDataDef;
import net.bytle.db.model.ColumnDef;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Iterator;

/**
 * Implementation of the CSV format
 *
 * @see <a href="https://commons.apache.org/proper/commons-csv/apidocs/org/apache/commons/csv/CSVFormat.html">CSVFormat for common format</a>
 *
 * <br>
 * Format:<br>
 * - <a href="https://www.w3.org/TR/2015/REC-tabular-data-model-20151217/#parsing">W3c Parsing</a><br>
 * - <a href="https://cloud.google.com/bigquery/docs/loading-data-cloud-storage-csv">BigQuery</a><br>
 */
public class CsvDataDef extends LineDataDef {

  private static final char DOUBLE_QUOTE = '"';


  /**
   * The path of the Csv
   */
  private final CsvDataPath fsDataPath;

  /**
   * The number of header rows in the file
   * Even without header, we create the structure (columns)
   */
  private int headerRowCount = 0;

  /**
   * Indicates whether to trim whitespace around cells
   * In case of fixed format
   */
  private boolean trimWhitespace = false;

  /**
   * See {@link #setNewLineCharacters(String)}
   */
  private String newLineCharacters = System.lineSeparator();

  /**
   * See {@link #setDelimiterCharacter(char) | Delimiter}
   */
  private char delimiterCharacter = ',';

  /**
   * See {@link #setEscapeCharacter(char)}
   */
  private char escapeCharacter = DOUBLE_QUOTE;

  /**
   * See {@link #setQuoteCharacter(char)}
   */
  private char quoteCharacter = DOUBLE_QUOTE;

  /**
   * See {@link #setIgnoreEmptyLine(boolean)}
   */
  private boolean isIgnoreEmptyLine = true;

  /**
   * See {@link #setCommentCharacter(char)}
   */
  private Character commentCharacter = '#';
  private boolean columnsWereBuild = false;

  /**
   * Set the comment character
   *
   * @param commentCharacter A character that, when it appears at the beginning of a row, indicates that the row is a comment - Default is null which means no rows are treated as comments
   * @return
   */
  public CsvDataDef setCommentCharacter(char commentCharacter) {
    this.commentCharacter = commentCharacter;
    return this;
  }


  /**
   * Ignore empty line
   *
   * @param ignoreEmptyLine if true, it will ignore empty line
   */
  public void setIgnoreEmptyLine(boolean ignoreEmptyLine) {
    isIgnoreEmptyLine = ignoreEmptyLine;
  }

  public boolean isTrimWhitespace() {
    return trimWhitespace;
  }

  public void setTrimWhitespace(boolean trimWhitespace) {
    this.trimWhitespace = trimWhitespace;
  }


  /**
   * Set the newline string
   *
   * @param newLineCharacters The strings that is used at the end of a row (default to the system default \r\n for Windows, \n for the other)
   */
  @Override
  public CsvDataDef setNewLineCharacters(String newLineCharacters) {
    super.setNewLineCharacters(newLineCharacters);
    return this;
  }

  public char getEscapeCharacter() {
    return escapeCharacter;
  }

  /**
   * Set the string that is used to escape the {@link #setQuoteCharacter(char)} quote character } within escaped cells
   *
   * @param escapeCharacter the string that is used to escape the {@link #setQuoteCharacter(char)}  quote character } within escaped cells
   * @return
   */
  public CsvDataDef setEscapeCharacter(char escapeCharacter) {
    this.escapeCharacter = escapeCharacter;
    return this;
  }

  public char getQuoteCharacter() {
    return quoteCharacter;
  }

  /**
   * @param quoteCharacter The string that is used around escaped cells
   */
  public CsvDataDef setQuoteCharacter(char quoteCharacter) {
    this.quoteCharacter = quoteCharacter;
    return this;
  }

  /**
   * @return the {@link #delimiterCharacter}
   */
  public char getDelimiterCharacter() {
    return delimiterCharacter;
  }

  /**
   * @param delimiter The separator between cells known as cell delimiter. Default value is a comma ','
   * @return The {@link CsvDataDef CsvDataDef} instance for chaining initialization
   */
  public CsvDataDef setDelimiterCharacter(char delimiter) {
    this.delimiterCharacter = delimiter;
    return this;
  }

  /**
   * Set the character set of the file
   *
   * @param charset The character encoding for the file - Default: UTf-8
   * @return The {@link CsvDataDef CsvDataDef} instance for chaining initialization
   */
  public CsvDataDef setCharset(Charset charset) {
    super.setCharset(charset);
    return this;
  }

  /**
   *
   * @return the header row number or 0 if it does not exist
   */
  public int getHeaderRowCount() {
    return headerRowCount;
  }

  public CsvDataDef setHeaderRowCount(int headerRowCount) {
    this.headerRowCount = headerRowCount;
    return this;
  }

  /**
   * @param dataPath The CsvDataPath
   */
  public CsvDataDef(CsvDataPath dataPath) {
    super(dataPath);
    this.fsDataPath = dataPath;
  }

  @Override
  public ColumnDef[] getColumnDefs() {
    return super.getColumnDefs();
  }

  @Override
  public <T> ColumnDef<T> getColumnDef(String columnName) {
    return super.getColumnDef(columnName);
  }

  @Override
  public <T> ColumnDef<T> getColumnDef(Integer columnIndex) {
    return super.getColumnDef(columnIndex);
  }

  @Override
  public int getColumnsSize() {
    return super.getColumnsSize();
  }

  @Override
  public CsvDataPath getDataPath() {
    return fsDataPath;
  }

  /**
   * Build the column metadata from the first row if needed
   * Lazy initialization
   */
  public void addColumnNamesFromHeader() {

    // The data structure was given in the definition
    if (!columnsWereBuild && super.getColumnsSize()!=0){
        columnsWereBuild = true;
    }

    if (!columnsWereBuild) {
      columnsWereBuild = true;
      if (Files.exists(fsDataPath.getNioPath())) {
        try (
          CSVParser csvParser = CSVParser.parse(fsDataPath.getNioPath(), charset, getCsvFormat());
        ) {
          Iterator<CSVRecord> recordIterator = csvParser.iterator();
          try {

            CSVRecord headerRecord = null;
            int iterate = (headerRowCount==0?1:headerRowCount);
            for (int i = 0; i < iterate; i++) {
              headerRecord = Csvs.safeIterate(recordIterator);
              if (headerRecord == null) {
                return;
              }
            }

            for (int i = 0; i < headerRecord.size(); i++) {
              if (headerRowCount > 0) {
                this.addColumn(headerRecord.get(i));
              } else {
                String columnName = String.valueOf(i + 1);
                this.addColumn(columnName);
              }
            }

          } catch (java.util.NoSuchElementException e) {
            // No more CSV records available, file is empty
            FsTableSystemLog.LOGGER_DB_FS.info("The file (" + fsDataPath.toString() + ") seems to be empty");
          }

        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }

  }

  /**
   * The format of the CSV file excepts the header
   * that is handled in the function {@link CsvManager#create(FsDataPath)}
   * This way we doesn't overwrite the file and we can add rows in an existing Csv file
   *
   * @return the Apache common Csv Format
   */
  protected CSVFormat getCsvFormat() {
    CSVFormat csvFormat = CSVFormat.DEFAULT
      .withDelimiter(delimiterCharacter)
      // Ignoring empty line means that it will just skip the line
      // if we have a comment or front matter above the header with empty line
      // there is no way to locate the header line precisely
      .withIgnoreEmptyLines(false)
      .withCommentMarker(commentCharacter)
      .withQuote(quoteCharacter)
      .withRecordSeparator(newLineCharacters);

    // If we set the escape character to double quote, we get an "Illegal state exception, EOF reach"
    if (escapeCharacter != DOUBLE_QUOTE) {
      csvFormat = csvFormat
        .withEscape(escapeCharacter);
    }
    return csvFormat;

  }


  public boolean isIgnoreEmptyLine() {
    return isIgnoreEmptyLine;
  }

  public char getCommentCharacter() {
    return commentCharacter;
  }
}
