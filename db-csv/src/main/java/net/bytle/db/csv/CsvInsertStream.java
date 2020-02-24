package net.bytle.db.csv;

import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.InsertStreamAbs;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.*;
import java.nio.file.*;
import java.util.List;

public class CsvInsertStream extends InsertStreamAbs implements InsertStream {

  private final CsvDataDef csvDataDef;
  final CSVPrinter printer;
  private final CsvDataPath csvDataPath;

  public CsvInsertStream(CsvDataPath fsDataPath) {

    super(fsDataPath);
    this.csvDataDef = fsDataPath.getDataDef();
    this.csvDataPath = fsDataPath;
    CSVFormat csvFormat = csvDataPath.getDataDef().getCsvFormat();
    String recordSeparator = csvFormat.getRecordSeparator();
    if (recordSeparator==null){
      csvFormat = csvFormat.withRecordSeparator(System.lineSeparator());
    }

    try {
      BufferedWriter writer = Files.newBufferedWriter(csvDataPath.getNioPath(), csvDataDef.getCharset(), StandardOpenOption.APPEND);
      printer = csvFormat
        .print(writer);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  public static CsvInsertStream of(CsvDataPath csvDataPath) {

    return new CsvInsertStream(csvDataPath);

  }


  /**
   * @param values - The values to insert
   * @return the {@link InsertStream} for insert chaining
   */
  @Override
  public CsvInsertStream insert(List<Object> values) {
    try {
      printer.printRecord(values);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Close the stream
   * ie
   * * commit the last rows
   * * close the connection
   * * ...
   */
  @Override
  public void close() {
    try {
      printer.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * In case of parent child hierarchy
   * we can check if we need to send the data with the function nextInsertSendBatch()
   * and send it with this function
   */
  @Override
  public void flush() {
    try {
      printer.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


}
