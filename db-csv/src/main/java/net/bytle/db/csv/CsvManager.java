package net.bytle.db.csv;

import net.bytle.db.fs.FsDataPath;
import net.bytle.db.fs.FsDataStore;
import net.bytle.db.fs.FsBinaryFileManager;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.fs.Fs;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

public class CsvManager extends FsBinaryFileManager {

  private static CsvManager csvManager;

  public static CsvManager getCsvManagerSingeleton() {
    if (csvManager == null) {
      csvManager = new CsvManager();
    }
    return csvManager;
  }

  @Override
  public CsvDataPath createDataPath(FsDataStore fsDataStore, Path path) {

    return new CsvDataPath(fsDataStore, path);

  }

  @Override
  public void create(FsDataPath fsDataPath) {

    CsvDataPath csvDataPath = (CsvDataPath) fsDataPath;
    Fs.createFile(csvDataPath.getNioPath());
    CsvDataDef csvDataDef = csvDataPath.getOrCreateDataDef();
    CSVFormat csvFormat = csvDataPath.getOrCreateDataDef().getCsvFormat();
    if (csvFormat.getRecordSeparator()==null){
      csvFormat = csvFormat.withRecordSeparator(System.lineSeparator());
    }
    if (csvDataDef.getHeaderRowCount() > 0) {
      final String[] headers = Arrays.stream(csvDataDef.getColumnDefs())
        .map(ColumnDef::getColumnName).toArray(String[]::new);
      if (headers.length != 0) {
        csvFormat = csvFormat.withHeader(headers);
      } else {
        throw new RuntimeException("The CSV file has a format asking to print the headers but there is no columns defined for this CSV");
      }
    }

    // Creation of the file with the header or not
    try {
      CSVPrinter printer = csvFormat
        .print(csvDataPath.getNioPath(), csvDataDef.getCharset());
      printer.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }


  @Override
  public SelectStream getSelectStream(FsDataPath fsDataPath) {

    return CsvSelectStream.of((CsvDataPath) fsDataPath);

  }

  @Override
  public InsertStream getInsertStream(FsDataPath fsDataPath) {
    return CsvInsertStream.of((CsvDataPath) fsDataPath);
  }

}
