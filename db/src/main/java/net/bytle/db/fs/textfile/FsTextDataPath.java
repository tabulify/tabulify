package net.bytle.db.fs.textfile;

import net.bytle.db.DbLoggers;
import net.bytle.db.fs.FsConnection;
import net.bytle.db.fs.FsDataPath;
import net.bytle.db.fs.binary.FsBinaryDataPath;
import net.bytle.db.fs.binary.FsBinaryFileManager;
import net.bytle.db.model.RelationDef;
import net.bytle.db.model.RelationDefDefault;
import net.bytle.db.spi.DataPath;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.transfer.TransferProperties;
import net.bytle.db.transfer.TransferSourceTarget;
import net.bytle.exception.*;
import net.bytle.fs.Fs;
import net.bytle.type.*;

import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static net.bytle.db.fs.textfile.FsTextDataPathAttributes.CHARACTER_SET;

/**
 * A text data path with a record tabular structure
 * where the record separator is the end of line
 * <p>
 * (one cell by row = one line)
 * Implementation of text format
 * A data path with a record tabular structure
 * where one record = one cell by row = one line
 */
public class FsTextDataPath extends FsBinaryDataPath implements FsDataPath {


  protected FsTextDataPath setColumnName(String name) {
    try {
      this.getVariable(FsTextDataPathAttributes.COLUMN_NAME).setOriginalValue(name);
    } catch (NoVariableException e) {
      throw new RuntimeException("Internal Error: COLUMN_NAME variable was not found. It should not happen");
    }
    return this;
  }


  protected static final String EOF = "\u001a";


  public FsTextDataPath(FsConnection fsConnection, Path path, MediaType mediaType) {
    super(fsConnection, path, mediaType);
    this.build();
  }

  public FsTextDataPath(FsConnection fsConnection, Path path) {
    super(fsConnection, path, MediaTypes.TEXT_PLAIN);
    this.build();
  }

  private void build() {

    this.addVariablesFromEnumAttributeClass(FsTextDataPathAttributes.class);

    this.getOrCreateVariable(CHARACTER_SET).setValueProvider(this::getCharsetDefault);
  }


  public static FsTextDataPath create(FsConnection fsConnection, Path path) {

    return new FsTextDataPath(fsConnection, path);

  }


  @Override
  public FsTextDataPath setDescription(String description) {
    super.setDescription(description);
    return this;
  }


  @Override
  public FsBinaryFileManager getFileManager() {
    return FsTextManager.getSingeleton();
  }


  /**
   * Character set values
   * They come from the character set detector
   * <a href="http://userguide.icu-project.org/conversion/detection#TOC-Detected-Encodings">...</a>
   * <p>
   * See also:
   * <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html">...</a>
   */
  public String[] getEndOfRecords() {

    try {
      return (String[]) this.getVariable(FsTextDataPathAttributes.END_OF_RECORD).getValueOrDefault();
    } catch (NoVariableException | NoValueException e) {
      throw IllegalArgumentExceptions.createFromMessage("This exception should not happen because there is already a default", e);
    }

  }

  /**
   * Set the newline string (ie the end of record)
   *
   * @param endOfRecords The strings that are used at the end of a row (default to the system default \r\n for Windows, \n for the other)
   */
  public FsTextDataPath setEndOfRecords(String... endOfRecords) {
    Variable variable = Variable.create(FsTextDataPathAttributes.END_OF_RECORD, Origin.INTERNAL).setOriginalValue(endOfRecords);
    this.addVariable(variable);
    return this;
  }


  /**
   * Set the character set of the file
   *
   * @param charset The character encoding for the file - Default: UTf-8
   * @return The {@link FsTextDataPath} instance for chaining initialization
   */
  public FsTextDataPath setCharset(Charset charset) {
    Variable variable = Variable.create(CHARACTER_SET, Origin.INTERNAL).setOriginalValue(charset);
    this.addVariable(variable);
    return this;
  }


  /**
   * @return the CharacterSet
   */
  public Charset getCharset() {


    try {
      return (Charset) this.getOrCreateVariable(CHARACTER_SET).getValueOrDefault();
    } catch (NoValueException e) {
      throw new InternalException("At minimal, the character set should have the default");
    }

  }

  public Charset getCharsetDefault() {

    /**
     * Charset detection if the file already exists
     */
    Path path = getNioPath();
    if (!Files.exists(path)) {
      return FsTextDataPathAttributes.DEFAULTS.CHARSET;
    }

    /**
     * Twirk if this is not a local file
     * We don't ask for the content to detect the
     * character set
     */
    if (!path.toUri().getScheme().equals("file")) {
      return FsTextDataPathAttributes.DEFAULTS.CHARSET;
    }

    String characterSet = Fs.detectCharacterSet(path);
    if (characterSet != null) {
      try {
        Charset charset = Casts.cast(characterSet, Charset.class);
        this.getOrCreateVariable(CHARACTER_SET).setProcessedValue(charset);
      } catch (CastException ex) {
        String message = "The string (" + characterSet + ") could not be transformed as characters set";
        if (getConnection().getTabular().isDev()) {
          throw new IllegalArgumentException(message);
        } else {
          DbLoggers.LOGGER_DB_ENGINE.warning(message);
        }
      }

    }
    return FsTextDataPathAttributes.DEFAULTS.CHARSET;

  }


  @Override
  public FsTextDataPath addVariable(String key, Object value) {

    FsTextDataPathAttributes textAtt;
    try {
      textAtt = Casts.cast(key, FsTextDataPathAttributes.class);
    } catch (Exception e) {
      super.addVariable(key, value);
      return this;
    }

    try {
      Variable variable = getConnection().getTabular().createVariable(textAtt, value);
      super.addVariable(variable);
      return this;
    } catch (Exception e) {
      throw new RuntimeException("Error while creating the variable (" + textAtt + ") with the value (" + value + ") for the resource (" + this + ")", e);
    }


  }

  @Override
  public Long getCount() {

    long i = 0;

    try (SelectStream selectStream = getSelectStream()) {
      while (selectStream.next()) {
        i++;
      }
    } catch (Exception e) {
      if (e.getCause() instanceof MalformedInputException) {
        FsTextLogger.LOGGER.fine("The row count of the file (" + this.getNioPath() + ") could not be calculated because we got the following error (" + e.getMessage() + ") while reading it. The file is not a text file or the known/detected character set (" + this.getCharset() + ") is not the good one.");
        return null;
      } else {
        throw e;
      }
    }
    return i;
  }

  @Override
  public InsertStream getInsertStream(DataPath source, TransferProperties transferProperties) {
    return FsTextInsertStream.create(this);
  }

  @Override
  public SelectStream getSelectStream() {
    return FsTextSelectStream.create(this);
  }


  @Override
  public RelationDef getOrCreateRelationDef() {
    if (relationDef == null) {
      relationDef = new RelationDefDefault(this);
      /**
       * A data path needs at minimum a column in a cross system transfer (ie fs to relation)
       * {@link TransferSourceTarget#sourcePreChecks()}
       * but it's created at runtime
       * <p>
       * Why ? for 2 reasons.
       * <p>
       * 1 - column name should be dynamic
       * Because we couldn't arrive to a meaningful name for this column
       * When people are choosing a column from a database to be the content of file
       * there is a column mapping and generally, the name of this column
       * is the name of the extension file.
       * <p>
       * Example, creating html files from a clob column named `html`
       * - name: "Store"
       *   operation: transfer
       *   args:
       *     target-uri: 'emails/${logicalName}.html@build'
       *     transfer-operation: insert
       *     transfer-column-mapping:
       *       "html": "html"
       *     step-granularity: "record"
       * We therefore don't add a pre-existing column.
       * If the column does not exist, it's created:
       *   * by the transfer manager
       *   * and if not at runtime, see {@link FsTextSelectStream#getRuntimeRelationDef()}
       *
       *
       * 2 - target transfer
       * if the text file have 1 column, there is no match at all when transfering
       * if there is no column, the transfer creates all the columns that it needs
       * pass the value
       * and the {@link FsTextInsertStream} correct the column structure at {@link FsTextInsertStream#close()} close }
       * time
       *
       */
      return relationDef;
    }
    return relationDef;
  }

  public String getColumnName() {

    try {
      return (String) this.getVariable(FsTextDataPathAttributes.COLUMN_NAME).getValueOrDefault();
    } catch (NoVariableException | NoValueException e) {
      throw new RuntimeException("Internal Error: COLUMN_NAME variable was not found. It should not happen");
    }

  }

  /**
   * A utility function that returns the content
   *
   * @return the text
   */
  public String getText() throws NoSuchFileException {

    return Fs.getFileContent(this.getNioPath(), this.getCharset());

  }

  public String getUniqueColumnName() {
    try {
      return (String) getVariable(FsTextDataPathAttributes.COLUMN_NAME).getValueOrDefault();
    } catch (NoVariableException | NoValueException e) {
      return FsTextDataPathAttributes.DEFAULTS.HEADER_DEFAULT;
    }
  }
}
