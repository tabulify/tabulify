package com.tabulify.fs.textfile;

import com.tabulify.model.RelationDef;
import com.tabulify.stream.SelectStreamAbs;
import net.bytle.exception.NoColumnException;
import net.bytle.type.Strings;
import net.bytle.type.TailQueue;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.sql.Clob;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class FsTextSelectStream extends SelectStreamAbs {


  private final List<Integer> eorLengthSizes;
  private BufferedReader bufferedReader;
  /**
   * The line number in the file
   */
  private long recordNumberInTextFile = 0;
  /**
   * The buffer
   * This is on a global level because we may
   * read character that are from the next record
   */
  private StringBuffer readBuffer = new StringBuffer();

  /**
   * The record
   */
  private String record;

  FsTextSelectStream(FsTextDataPath fsTextDataPath) {

    super(fsTextDataPath);

    eorLengthSizes = Arrays.stream(getDataPath().getEndOfRecords())
      .map(String::length).distinct().collect(Collectors.toList());

    beforeFirst();
  }

  @Override
  public FsTextDataPath getDataPath() {
    return (FsTextDataPath) super.getDataPath();
  }

  public static FsTextSelectStream create(FsTextDataPath fsTextDataPath) {

    return new FsTextSelectStream(fsTextDataPath);

  }


  @Override
  public boolean next() {

    record = readAndGetRecord();
    if (record == null) {
      return false;
    } else {
      recordNumberInTextFile++;
      return true;
    }

  }

  /**
   * @return a record or null if no data anymore
   */
  private String readAndGetRecord() {
    try {


      /**
       * A fixed queue that delete the head
       * when a character is added
       * If the character in this queue are equals to the end of characters
       * the record is returned
       */
      List<TailQueue<Character>> endOfRecordBuffers = new ArrayList<>();
      this.eorLengthSizes.forEach(size -> endOfRecordBuffers.add(new TailQueue<>(size)));

      /**
       * The end of record found to handle
       * Case such as \r\n
       * where \r is first seen as an EOR
       * and stays \n that is also seen as a EOR
       */
      String previousEndOfRecordFound = null;

      /**
       * The processing to extract the record
       */
      while (true) {
        /**
         * The next character
         */
        int c = bufferedReader.read();

        /**
         * If no more characters
         */
        if (c == -1) {
          if (readBuffer.length() == 0) {
            return null;
          } else {
            return getRecordAndResetBuffer(previousEndOfRecordFound, null);
          }
        }

        /**
         * Loop Stop ?
         */
        boolean endOfRecordFound = false;
        for (TailQueue<Character> endOfRecordBuffer : endOfRecordBuffers) {
          endOfRecordBuffer.add((char) c);
          String eorBufferAsString = Strings.createFromCharacters(new ArrayList<>(endOfRecordBuffer)).toString();
          if (Arrays.asList(this.getDataPath().getEndOfRecords()).contains(eorBufferAsString)) {
            /**
             * Case of \r\n
             * where \r is first seen as an EOR
             * and stays \n that is also seen as a EOR
             */
            previousEndOfRecordFound = eorBufferAsString;
            endOfRecordFound = true;
          }
        }

        if (!endOfRecordFound && previousEndOfRecordFound != null) {
          return getRecordAndResetBuffer(previousEndOfRecordFound, c);
        }

        /**
         * Not stopped
         */
        readBuffer.append((char) c);


      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  private String getRecordAndResetBuffer(String previousEndOfRecordFound, Integer previousCharacter) {
    String record;
    if (previousEndOfRecordFound != null) {
      record = this.readBuffer.delete(this.readBuffer.length() - previousEndOfRecordFound.length(), this.readBuffer.length()).toString();
    } else {
      record = this.readBuffer.toString();
    }
    this.readBuffer = new StringBuffer();
    if (previousCharacter != null) {
      this.readBuffer.append((char) (int) previousCharacter);
    }
    return record;
  }


  @Override
  public void close() {
    try {
      bufferedReader.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getString(int columnIndex) {

    return safeGet(columnIndex);

  }

  private String safeGet(int columnIndex) {
    if (columnIndex == 1) {
      return record;
    } else {
      throw new RuntimeException("The file ("+this.getDataPath().getName()+") has been loaded as a text file. Therefore, you can ask only the column (1) not the column (" + columnIndex + "). If the file has a structured format, the plugin should be enabled.");
    }
  }


  @Override
  public void beforeFirst() {
    try {
      recordNumberInTextFile = 0;
      Charset charset = getDataPath().getCharset();
      bufferedReader = Files.newBufferedReader(getDataPath().getAbsoluteNioPath(), charset);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public long getRow() {
    return recordNumberInTextFile;
  }


  @Override
  public Object getObject(int columnIndex) {
    return safeGet(columnIndex);
  }

  /**
   * @return the runtime relation def
   */
  @Override
  public RelationDef getRuntimeRelationDef() {

    RelationDef relationDef = this.getDataPath().getOrCreateRelationDef();
    if (relationDef.getColumnsSize() == 0) {
      // No transfer has defined the header (one column only)
      relationDef.addColumn(this.getDataPath().getUniqueColumnName());
    }
    return relationDef;
  }


  @Override
  public Double getDouble(int columnIndex) {

    String s = safeGet(columnIndex);
    if (s == null) {
      return null;
    } else {
      return Double.parseDouble(s);
    }

  }

  @Override
  public Clob getClob(int columnIndex) {
    throw new UnsupportedOperationException("Not Yet Supported");
  }


  /**
   * Retrieves and removes the head of this data path, or returns false if this queue is empty.
   *
   * @param timeout  - the time out before returning null
   * @param timeUnit - the time unit of the time out
   * @return true if there is a new element, otherwise false
   */
  @Override
  public boolean next(Integer timeout, TimeUnit timeUnit) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public List<String> getObjects() {
    return Collections.singletonList(record);
  }

  @Override
  public Integer getInteger(int columnIndex) {
    String s = safeGet(columnIndex);
    if (s == null) {
      return null;
    } else {
      return Integer.parseInt(s);
    }
  }

  @Override
  public Object getObject(String columnName) {
    try {
      return getObject(this.getDataPath().getOrCreateRelationDef().getColumnDef(columnName).getColumnPosition());
    } catch (NoColumnException e) {
      throw new RuntimeException("The column name ("+columnName+") was not found in the resource ("+this.getDataPath()+"), we could not retrieve the value.");
    }
  }


}
