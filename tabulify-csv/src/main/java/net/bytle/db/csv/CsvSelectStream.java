package net.bytle.db.csv;

import net.bytle.db.model.RelationDef;
import net.bytle.db.stream.SelectStreamAbs;
import net.bytle.exception.CastException;
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
    } else {
      lineNumberInTextFile++;
      if (currentRecord.size() == 1) {
        // Empty line
        if (currentRecord.get(0).isEmpty() && csvDataPath.isIgnoreEmptyLine()) {
          return safeIterate();
        } else {
          return true;
        }
      } else {
        return true;
      }
    }

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
  public String getString(int columnIndex) {

    return safeGet(columnIndex);

  }

  private String safeGet(int columnIndex) {
    if (rowNum == 0) {
      throw new RuntimeException("You need to go the the first row with the next() function before asking for an object");
    }
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

      if (!csvDataPath.getConnection().getTabular().isStrict()) {
        String quoteCharacter = "\"";
        if (value.substring(0, 1).equals(quoteCharacter)) {
          if (value.substring(value.length() - 1).equals(quoteCharacter)) {
            value = value.substring(1, value.length() - 1);
          }
        }
      }

      return value;
    } catch (Exception e) {
      throw new RuntimeException("Error on the record (" + getRow() + ") when trying to retrieve the column (" + columnIndex + "). Records values are (" + currentRecord + ") ", e);
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
      while (lineNumberInTextFile < dataDef.getHeaderRowId()) {
        safeIterate();
      }

      rowNum = 0;

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public long getRow() {
    return rowNum;
  }


  @Override
  public Object getObject(int columnIndex) {
    return safeGet(columnIndex);
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
  public Double getDouble(int columnIndex) {

    String s = safeGet(columnIndex);
    if (s == null) {
      return null;
    } else {
      try {
        return Casts.cast(s, Double.class);
      } catch (CastException e) {
        throw new RuntimeException("The value (" + s + ") on in the resource (" + this.getDataPath() + ") in the column (" + this.getDataPath().getRelationDef().getColumnDef(columnIndex).getColumnName() + ") at the row (" + rowNum + ") is not a double", e);
      }
    }

  }

  @Override
  public Clob getClob(int columnIndex) {
    throw new RuntimeException("Not Yet implemented");
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
    return IntStream.range(0, currentRecord.size())
      .mapToObj(currentRecord::get)
      .collect(Collectors.toList());
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
    return currentRecord.get(columnName);
  }

  @Override
  public CsvDataPath getDataPath() {
    return (CsvDataPath) super.getDataPath();
  }
}
