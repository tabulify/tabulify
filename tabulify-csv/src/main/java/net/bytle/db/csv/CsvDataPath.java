package net.bytle.db.csv;

import net.bytle.db.fs.FsConnection;
import net.bytle.db.fs.FsDataPath;
import net.bytle.db.fs.FsLog;
import net.bytle.db.fs.textfile.FsTextDataPath;
import net.bytle.db.model.RelationDef;
import net.bytle.db.model.RelationDefDefault;
import net.bytle.db.spi.DataPath;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.transfer.TransferProperties;
import net.bytle.exception.InternalException;
import net.bytle.exception.NoValueException;
import net.bytle.exception.NoVariableException;
import net.bytle.type.Casts;
import net.bytle.type.MediaTypes;
import net.bytle.type.Origin;
import net.bytle.type.Variable;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
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
public class CsvDataPath extends FsTextDataPath {


  public CsvDataPath(FsConnection fsConnection, Path path) {
    super(fsConnection, path, MediaTypes.TEXT_CSV);

    this.addVariablesFromEnumAttributeClass(CsvDataPathAttribute.class);

  }


  /**
   * If this variable is true,
   * the file was scanned
   */
  private boolean columnsWereBuild = false;


  /**
   * Indicates whether to trim whitespace around cells
   * In case of fixed format
   */
  private boolean trimWhitespace = false;


  /**
   * Set the comment character.
   *
   * @param commentCharacter A character that, when it appears at the beginning of a row, indicates that the row is a comment - Default is null which means no rows are treated as comments
   * @return the resource
   */
  public CsvDataPath setCommentCharacter(char commentCharacter) {
    try {
      this.getVariable(CsvDataPathAttribute.COMMENT_CHARACTER).setOriginalValue(commentCharacter);
      return this;
    } catch (NoVariableException e) {
      throw new InternalException("The COMMENT_CHARACTER has already a default, it should not happen");
    }

  }


  /**
   * Ignore empty line
   * <p>
   * Implemented in the select stream {@link CsvSelectStream#safeIterate()} and not via the third part
   * <p>
   * Because Ignoring empty line with the third library implementation means that it will just skip the line
   * without giving us this knowledge back.
   * If we have a comment or front matter above the header with an empty line
   * There is no way to locate the header line precisely
   * Ignoring empty line is then done in the select stream {@link CsvSelectStream#safeIterate()}
   *
   * @param ignoreEmptyLine the boolean value
   */
  public CsvDataPath setIgnoreEmptyLine(boolean ignoreEmptyLine) {

    try {
      this.getVariable(CsvDataPathAttribute.IGNORE_EMPTY_LINE).setOriginalValue(ignoreEmptyLine);
      return this;
    } catch (NoVariableException e) {
      throw new InternalException("The IGNORE_EMPTY_LINE has already a default, it should not happen");
    }


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
   * @param endOfRecords The strings that is used at the end of a row (default to the system default \r\n for Windows, \n for the other)
   */
  @Override
  public CsvDataPath setEndOfRecords(String... endOfRecords) {
    super.setEndOfRecords(endOfRecords);
    return this;
  }

  public Character getEscapeCharacter() {

    try {
      return (Character) this.getVariable(CsvDataPathAttribute.ESCAPE_CHARACTER).getValueOrDefaultOrNull();
    } catch (NoVariableException e) {
      throw new InternalException("The ESCAPE_CHARACTER has already a default and should be added, it should not happen", e);
    }

  }

  /**
   * Set the string that is used to escape the {@link #setQuoteCharacter(char)} quote character } within escaped cells
   *
   * @param escapeCharacter the string that is used to escape the {@link #setQuoteCharacter(char)}  quote character } within escaped cells
   * @return the resource
   */
  public CsvDataPath setEscapeCharacter(char escapeCharacter) {

    try {
      this.getVariable(CsvDataPathAttribute.ESCAPE_CHARACTER).setOriginalValue(escapeCharacter);
      return this;
    } catch (NoVariableException e) {
      throw new InternalException("The ESCAPE_CHARACTER has already a default and should be added, it should not happen", e);
    }

  }

  public Character getQuoteCharacter() {

    try {
      return (Character) this.getVariable(CsvDataPathAttribute.QUOTE_CHARACTER).getValueOrDefault();
    } catch (NoVariableException | NoValueException e) {
      throw new InternalException("The QUOTE_CHARACTER has already a default and should be added, it should not happen", e);
    }
  }

  /**
   * @param quoteCharacter The string that is used around escaped cells
   */
  public CsvDataPath setQuoteCharacter(char quoteCharacter) {
    try {
      this.getVariable(CsvDataPathAttribute.QUOTE_CHARACTER).setOriginalValue(quoteCharacter);
      return this;
    } catch (NoVariableException e) {
      throw new InternalException("The QUOTE_CHARACTER has already a default and should be added, it should not happen", e);
    }

  }

  /**
   * @return the delimiter
   */
  public Character getDelimiterCharacter() {

    try {
      return (Character) this.getVariable(CsvDataPathAttribute.DELIMITER_CHARACTER).getValueOrDefault();
    } catch (NoVariableException | NoValueException e) {
      throw new InternalException("The DELIMITER_CHARACTER has already a default and should be added, it should not happen", e);
    }

  }

  /**
   * @param delimiter The separator between cells known as cell delimiter. Default value is a comma ','
   * @return The {@link CsvDataPath CsvDataPath} instance for chaining initialization
   */
  public CsvDataPath setDelimiterCharacter(char delimiter) {
    try {
      this.getVariable(CsvDataPathAttribute.DELIMITER_CHARACTER).setOriginalValue(delimiter);
      return this;
    } catch (NoVariableException e) {
      throw new InternalException("The DELIMITER_CHARACTER has already a default and should be added, it should not happen", e);
    }

  }

  /**
   * Set the character set of the file
   *
   * @param charset The character encoding for the file - Default: UTf-8
   * @return The {@link CsvDataPath CsvDataPath} instance for chaining initialization
   */
  public CsvDataPath setCharset(Charset charset) {
    super.setCharset(charset);
    return this;
  }

  /**
   * @return the header row number (0, no header)
   */
  public int getHeaderRowId() {

    try {
      return (int) this.getVariable(CsvDataPathAttribute.HEADER_ROW_ID).getValueOrDefault();
    } catch (NoVariableException | NoValueException e) {
      throw new InternalException("The HEADER_ROW_ID has already a default and should be added, it should not happen", e);
    }
  }

  public CsvDataPath setHeaderRowId(int headerRowId) {
    try {
      this.getVariable(CsvDataPathAttribute.HEADER_ROW_ID).setOriginalValue(headerRowId);
      return this;
    } catch (NoVariableException e) {
      throw new InternalException("The HEADER_ROW_ID has already a default and should be added, it should not happen", e);
    }
  }


  @Override
  public CsvDataPath addVariable(String key, Object value) {


    assert value != null;
    assert value.toString().length() > 0 : "The value of a CSV property should have at minimal a length of 1";

    CsvDataPathAttribute csvDataPathAttribute;
    try {
      csvDataPathAttribute = Casts.cast(key, CsvDataPathAttribute.class);
    } catch (Exception e) {
      // It may be a text property
      super.addVariable(key, value);
      return this;
    }

    Variable variable = Variable.create(csvDataPathAttribute, Origin.INTERNAL).setOriginalValue(value);
    super.addVariable(variable);

    return this;

  }


  /**
   * Build the column metadata if needed
   */
  public void buildColumnNamesFromFileIfNeeded() {

    RelationDef relationDef = getOrCreateRelationDef();
    if (!columnsWereBuild) {
      columnsWereBuild = true;
      Path nioPath = getAbsoluteNioPath();
      if (Files.isDirectory(nioPath)) {
        throw new RuntimeException("The file (" + nioPath + ") is a directory, not a csv file.");
      }
      if (Files.exists(nioPath)) {

        long lineNumberInTextFile = 0;
        try (
          CSVParser csvParser = CSVParser.parse(nioPath, this.getCharset(), this.getCsvFormat())
        ) {
          Iterator<CSVRecord> recordIterator = csvParser.iterator();
          try {

            /*
             * Iterate to get:
             * * the first header row
             * * or at least the first row
             */
            CSVRecord headerRecord = null;
            if (this.getHeaderRowId() == 0) {
              headerRecord = safeIterate(recordIterator);
            } else {
              while (lineNumberInTextFile < this.getHeaderRowId()) {
                headerRecord = safeIterate(recordIterator);
                lineNumberInTextFile++;
              }
            }

            if (headerRecord == null) {
              return;
            }
            int size = headerRecord.size();
            //noinspection SwitchStatementWithTooFewBranches
            switch (size) {
              case 1:
                if (this.getHeaderRowId() > 0) {
                  relationDef.addColumn(headerRecord.get(0));
                } else {
                  relationDef.addColumn("Lines");
                }
                break;
              default:
                // A prefix to not have the automatic number conversion problem
                // ie select 1 from will return 1
                String colPrefix = "col";
                for (int i = 0; i < size; i++) {
                  /*
                   * If there is a header
                   */
                  if (this.getHeaderRowId() > 0) {
                    String columnName = headerRecord.get(i);

                    // Suppress ""
                    if (columnName.length() > 1) {
                      if (columnName.charAt(0) == '"' && columnName.charAt(columnName.length() - 1) == '"') {
                        columnName = columnName.substring(1, columnName.length() - 1);
                      }
                    }

                    if (columnName.trim().isEmpty()) {
                      columnName = colPrefix + (i + 1);
                    }
                    relationDef.addColumn(columnName);

                  } else {
                    /*
                     * No header
                     */
                    String columnName = colPrefix + (i + 1);
                    relationDef.addColumn(columnName);
                  }
                }
            }


          } catch (java.util.NoSuchElementException e) {
            // No more CSV records available, file is empty
            FsLog.LOGGER_DB_FS.info("The file (" + nioPath + ") seems to be empty");
          }

        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

    }

  }

  /**
   * @param recordIterator the iterator
   * @return a csvRecord or null if this is the end
   */
  public CSVRecord safeIterate(Iterator<CSVRecord> recordIterator) {

    try {
      if (recordIterator.hasNext()) {
        return recordIterator.next();
      } else {
        return null;
      }
    } catch (Exception e) {
      // No record could be found
      if (e.getMessage().equals("IOException reading next record: java.io.IOException: (startline 1) EOF reached before encapsulated token finished")) {
        throw new RuntimeException("The end of the file was found before the end of the record. You should verify that the characters. They should not be equals and the end of line should exists.", e);
      } else if (e.getMessage().equals("IOException reading next record: java.io.IOException: (line 1) invalid char between encapsulated token and delimiter")) {
        throw new RuntimeException("The csv delimiter argument is `" + this.getDelimiterCharacter() + "`. Are your sure that this is the one in the file ? The error received is:" + e.getMessage(), e);
      } else {
        throw e;
      }
    }

  }

  /**
   * The format of the CSV file excepts the header
   * that is handled in the function {@link CsvManagerFs#create(FsDataPath)}
   * This way we doesn't overwrite the file and we can add rows in an existing Csv file
   *
   * @return the Apache common Csv Format
   */
  protected CSVFormat getCsvFormat() {

    Character delimiterCharacter = this.getDelimiterCharacter();
    CSVFormat csvFormat = CSVFormat.newFormat(delimiterCharacter);


    // Always false see the set function for the `why`
    csvFormat = csvFormat.withIgnoreEmptyLines(false);

    // Comment character for the third library even if not set is the #
    // We then have the same by default
    char commentCharacter = this.getCommentCharacter();
    csvFormat = csvFormat.withCommentMarker(commentCharacter);

    Character quoteCharacter = this.getQuoteCharacter();
    if (quoteCharacter != null) {
      if (quoteCharacter.equals(commentCharacter)) {
        throw new RuntimeException("The quote character (" + quoteCharacter + ") is the same than the comment character (" + commentCharacter + ") and that's not permitted");
      }
      csvFormat = csvFormat.withQuote(quoteCharacter);
    }
    if (getEndOfRecords() != null) {
      csvFormat = csvFormat.withRecordSeparator(getFirstEndOfRecords());
    }

    // If we set the escape character to double quote, we get an "Illegal state exception, EOF reach"
    Character escapeCharacter = this.getEscapeCharacter();
    if (escapeCharacter != null) {
      if (quoteCharacter != null && quoteCharacter.equals(escapeCharacter)) {
        CsvLogger.LOGGER.fine(
          "The quote character (" + quoteCharacter + ") is the same than the escape character (" + escapeCharacter + ").",
          "We don't set it because otherwise an error will occurs",
          "This is the default value anyway. Quoted value will work"
        );
      } else {
        csvFormat = csvFormat.withEscape(escapeCharacter);
      }
    }


    // Ignore empty line is not implemented within the library
    // See the setIgnoreEmpty line documentation for more

    return csvFormat;

  }

  private String getFirstEndOfRecords() {
    return getEndOfRecords()[0];
  }


  public boolean isIgnoreEmptyLine() {

    try {
      return (boolean) this.getVariable(CsvDataPathAttribute.IGNORE_EMPTY_LINE).getValueOrDefault();
    } catch (NoVariableException | NoValueException e) {
      throw new InternalException("The IGNORE_EMPTY_LINE has already a default and should be added, it should not happen", e);
    }

  }

  public char getCommentCharacter() {
    try {
      return (char) this.getVariable(CsvDataPathAttribute.COMMENT_CHARACTER).getValueOrDefault();
    } catch (NoVariableException | NoValueException e) {
      throw new InternalException("The COMMENT_CHARACTER has already a default and should be added, it should not happen", e);
    }
  }

  public static CsvDataPath createFrom(FsConnection fsConnection, Path path) {

    return new CsvDataPath(fsConnection, path);

  }


  @Override
  public RelationDef getOrCreateRelationDef() {
    if (this.relationDef == null) {
      this.relationDef = new RelationDefDefault(this);
      buildColumnNamesFromFileIfNeeded();
    }
    return this.relationDef;
  }

  @Override
  public CsvDataPath setDescription(String description) {
    super.setDescription(description);
    return this;
  }

  @Override
  public CsvManagerFs getFileManager() {
    return CsvManagerFs.getCsvManagerSingleton();
  }


  @Override
  public SelectStream getSelectStream() {
    return CsvSelectStream.of(this);
  }

  @Override
  public InsertStream getInsertStream(DataPath source, TransferProperties transferProperties) {
    return CsvInsertStream.of(this);
  }

}
