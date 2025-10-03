package com.tabulify.csv;

import com.tabulify.model.ColumnDef;
import com.tabulify.model.RelationDef;
import com.tabulify.stream.SelectStreamAbs;
import net.bytle.type.Casts;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Clob;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CsvSelectStream extends SelectStreamAbs {


  private final CsvDataPath csvDataPath;
  private Iterator<CSVRecord> recordIterator;
  private CSVParser csvParser;
  private CSVRecord currentRecord;

  /**
   * The record number (0=first)
   */
  private long rowNum = 0;


  /**
   * The line number in the file
   */
  private long lineNumberInTextFile = 0;

  CsvSelectStream(CsvDataPath csvDataPath) {

    super(csvDataPath);
    this.csvDataPath = csvDataPath;

    beforeFirst();

  }

  public static CsvSelectStream of(CsvDataPath csvDataPath) {

    return new CsvSelectStream(csvDataPath);

  }


  @Override
  public boolean next() {
    boolean recordWasFetched = safeIterate();
    if (recordWasFetched) {
      rowNum++;
    }
    return recordWasFetched;
  }

  /**
   * The file may empty, it throws then exception,
   * this utility method encapsulates it
   *
   * @return true if there is another record, false otherwise
   */
  protected boolean safeIterate() {

    currentRecord = this.getDataPath().safeIterate(recordIterator);
    if (currentRecord == null) {
      return false;
    }

    lineNumberInTextFile++;

    // Empty line
    if (currentRecord.size() == 1 && currentRecord.get(0).isEmpty() && csvDataPath.isIgnoreEmptyLine()) {
      return safeIterate();
    }

    return true;

  }

  @Override
  public void close() {
    try {
      csvParser.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isClosed() {
    return csvParser.isClosed();
  }


  private Object safeGet(ColumnDef<?> columnDef) {
    if (rowNum == 0) {
      throw new RuntimeException("You need to go the the first row with the next() function before asking for an object");
    }
    int columnIndex = columnDef.getColumnPosition();
    if (columnIndex > currentRecord.size()) {
      final int size = csvDataPath.getOrCreateRelationDef().getColumnsSize();
      if (currentRecord.size() > size) {
        throw new RuntimeException("There is no data at the index (" + columnIndex + ") because this tabular has (" + size + ") columns (Column 1 is at index 0).");
      } else {
        return null;
      }
    }
    try {
      String value = currentRecord.get(columnIndex - 1);

      if (!csvDataPath.getConnection().getTabular().isStrictExecution()) {
        String quoteCharacter = "\"";
        if (value.substring(0, 1).equals(quoteCharacter)) {
          if (value.substring(value.length() - 1).equals(quoteCharacter)) {
            value = value.substring(1, value.length() - 1);
          }
        }
      }

      return Casts.cast(value, columnDef.getClazz());

    } catch (Exception e) {
      throw new RuntimeException("Error on the record (" + getRecordId() + ") when trying to retrieve the column (" + columnIndex + "). Records values are (" + currentRecord + ") ", e);
    }
  }

  @Override
  public void beforeFirst() {
    try {

      CsvDataPath dataDef = (CsvDataPath) this.getRuntimeRelationDef().getDataPath();
      CSVFormat csvFormat = dataDef.getCsvFormat();
      Path nioPath = csvDataPath.getAbsoluteNioPath();
      csvParser = CSVParser.parse(nioPath, dataDef.getCharset(), csvFormat);
      recordIterator = csvParser.iterator();
      lineNumberInTextFile = 0;

      // Pass the header
      boolean hasNext = true;
      int headerRowId = dataDef.getHeaderRowId();
      while (hasNext) {
        if (!(lineNumberInTextFile < headerRowId)) break;
        hasNext = safeIterate();
      }

      rowNum = 0;

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public long getRecordId() {
    return rowNum;
  }


  @Override
  public Object getObject(ColumnDef columnDef) {
    return safeGet(columnDef);
  }

  /**
   * Will add the columns names from the header if there is no columns
   * <p>
   * {@link #beforeFirst()} initiate it at run time data definition
   */
  @Override
  public RelationDef getRuntimeRelationDef() {
    return this.csvDataPath.getOrCreateRelationDef();
  }


  @Override
  public Clob getClob(int columnIndex) {
    throw new RuntimeException("Not Yet implemented");
  }


  /**
   * Retrieves and removes the head of this data path, or returns false if this queue is empty.
   *
   * @param timeout  - the timeout before returning null
   * @param timeUnit - the time unit of the timeout
   * @return true if there is a new element, otherwise false
   */
  @Override
  public boolean next(Integer timeout, TimeUnit timeUnit) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public List<String> getObjects() {
    return IntStream.range(0, currentRecord.size())
      .mapToObj(currentRecord::get)
      .collect(Collectors.toList());
  }


  @Override
  public CsvDataPath getDataPath() {
    return (CsvDataPath) super.getDataPath();
  }
}
