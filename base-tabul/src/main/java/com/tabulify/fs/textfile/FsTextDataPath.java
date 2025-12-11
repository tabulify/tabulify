package com.tabulify.fs.textfile;

import com.tabulify.DbLoggers;
import com.tabulify.conf.Attribute;
import com.tabulify.fs.FsConnection;
import com.tabulify.fs.FsDataPath;
import com.tabulify.fs.binary.FsBinaryDataPath;
import com.tabulify.fs.binary.FsBinaryFileManager;
import com.tabulify.model.RelationDef;
import com.tabulify.model.RelationDefDefault;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.text.plain.TextDetectedCharsetNotSupported;
import com.tabulify.text.plain.TextFile;
import com.tabulify.transfer.TransferPropertiesSystem;
import com.tabulify.transfer.TransferSourceTargetOrder;
import com.tabulify.exception.IllegalArgumentExceptions;
import com.tabulify.exception.NoVariableException;
import com.tabulify.fs.Fs;
//#import com.tabulify.fs.FsTextDetectedCharsetNotSupported;
//import com.tabulify.fs.FsTextFile;
import com.tabulify.type.Casts;
import com.tabulify.type.KeyNormalizer;
import com.tabulify.type.MediaType;
import com.tabulify.type.MediaTypes;

import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static com.tabulify.conf.Origin.DEFAULT;
import static com.tabulify.fs.textfile.FsTextDataPathAttributes.CHARACTER_SET;

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
      this.getAttribute(FsTextDataPathAttributes.COLUMN_NAME).setPlainValue(name);
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

    if (path.isAbsolute()) {
      Path nioPath = fsConnection.getNioPath();
      try {
        path = Fs.relativize(path, nioPath);
      } catch (Exception e) {
        throw new IllegalArgumentException("The path " + path + " cannot be relativized from the connection path " + nioPath, e);
      }
    }
    return new FsTextDataPath(fsConnection, path);

  }


  @Override
  public FsTextDataPath setComment(String description) {
    super.setComment(description);
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
      return (String[]) this.getAttribute(FsTextDataPathAttributes.END_OF_RECORD).getValueOrDefault();
    } catch (NoVariableException e) {
      throw IllegalArgumentExceptions.createFromMessage("This exception should not happen because there is already a default", e);
    }

  }

  /**
   * Set the newline string (ie the end of record)
   *
   * @param endOfRecords The strings that are used at the end of a row (default to the system default \r\n for Windows, \n for the other)
   */
  public FsTextDataPath setEndOfRecords(String... endOfRecords) {
    Attribute attribute = Attribute.create(FsTextDataPathAttributes.END_OF_RECORD, DEFAULT).setPlainValue(endOfRecords);
    this.addAttribute(attribute);
    return this;
  }


  /**
   * Set the character set of the file
   *
   * @param charset The character encoding for the file - Default: UTf-8
   * @return The {@link FsTextDataPath} instance for chaining initialization
   */
  public FsTextDataPath setCharset(Charset charset) {
    Attribute attribute = Attribute.create(CHARACTER_SET, DEFAULT).setPlainValue(charset);
    this.addAttribute(attribute);
    return this;
  }


  /**
   * @return the CharacterSet
   */
  public Charset getCharset() {

    // should not return null as character set as a default
    return (Charset) this.getOrCreateVariable(CHARACTER_SET).getValueOrDefault();

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

    Charset characterSet = null;
    try {
      characterSet = TextFile.builder(path).detectCharacterSet();
    } catch (TextDetectedCharsetNotSupported e) {
      // character set detected but the name is not supported
      String message = "The character set detected for the resource (" + this + ") is not supported on this operating system. Error: " + e.getMessage();
      if (getConnection().getTabular().isStrictExecution()) {
        throw new IllegalArgumentException(message, e);
      } else {
        DbLoggers.LOGGER_DB_ENGINE.warning(message);
      }
    }
    if (characterSet != null) {
      this.getOrCreateVariable(CHARACTER_SET).setPlainValue(characterSet);
    }

    return FsTextDataPathAttributes.DEFAULTS.CHARSET;

  }


  @Override
  public FsTextDataPath addAttribute(KeyNormalizer key, Object value) {

    FsTextDataPathAttributes textAtt;
    try {
      textAtt = Casts.cast(key, FsTextDataPathAttributes.class);
    } catch (Exception e) {
      super.addAttribute(key, value);
      return this;
    }
    try {
      Attribute attribute = getConnection().getTabular().getVault().createAttribute(textAtt, value, DEFAULT);
      super.addAttribute(attribute);
      return this;
    } catch (Exception e) {
      throw new RuntimeException("Error while creating the variable (" + textAtt + ") with the value (" + value + ") for the resource (" + this + ")", e);
    }


  }

  @Override
  public Long getCount() {

    try {
      return super.getCount();
    } catch (Exception e) {
      if (e.getCause() instanceof MalformedInputException) {
        throw new RuntimeException("The row count of the file (" + this.getNioPath() + ") could not be calculated because we got the following error (" + e.getMessage() + ") while reading it. The file is not a text file or the known/detected character set (" + this.getCharset() + ") is not the good one.", e);
      }
      throw e;
    }

  }

  @Override
  public InsertStream getInsertStream(DataPath source, TransferPropertiesSystem transferProperties) {
    return FsTextInsertStream.create(this);
  }

  @Override
  public SelectStream getSelectStream() {
    return FsTextSelectStream.create(this);
  }


  @Override
  public RelationDef createRelationDef() {

    /**
     * A text data path is the only free form structure
     * Meaning that this is the only structure that allows a many-to-one insertion
     * ie many columns to only one line.
     * <p>
     * The transfer manages therefore the structure and is aware of this fact
     * <p>
     * A data path needs at minimum a column in a cross system transfer (ie fs to relation)
     * {@link TransferSourceTargetOrder#sourcePreChecks()}
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
     * if the text file has 1 column, there is no match at all when transferring
     * if there is no column, the transfer creates all the columns that it needs
     * pass the value
     * and the {@link FsTextInsertStream} correct the column structure at {@link FsTextInsertStream#close()} close
     * time
     * <p>
     *
     * 3 - when the file already exists, the transfer into a text file may be many to one
     * (ie many column creating one line)
     */
    relationDef = new RelationDefDefault(this);
    String uniqueColumnName = this.getColumnName();
    relationDef.addColumn(uniqueColumnName);
    return relationDef;

  }

  public String getColumnName() {

    try {
      return (String) this.getAttribute(FsTextDataPathAttributes.COLUMN_NAME).getValueOrDefault();
    } catch (NoVariableException e) {
      throw new RuntimeException("Internal Error: COLUMN_NAME variable was not found. It should not happen");
    }

  }

  /**
   * A utility function that returns the content
   *
   * @return the text
   */
  public String getText() throws NoSuchFileException {

    return Fs.getFileContent(this.getAbsoluteNioPath(), this.getCharset());

  }

  public String getUniqueColumnName() {
    try {
      return (String) getAttribute(FsTextDataPathAttributes.COLUMN_NAME).getValueOrDefault();
    } catch (NoVariableException e) {
      return FsTextDataPathAttributes.DEFAULTS.HEADER_DEFAULT;
    }
  }


}
