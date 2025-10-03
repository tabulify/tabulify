package com.tabulify.csv;

import com.tabulify.fs.FsConnection;
import com.tabulify.fs.FsDataPath;
import com.tabulify.fs.textfile.FsTextManager;
import com.tabulify.model.ColumnDef;
import net.bytle.fs.Fs;
import net.bytle.type.MediaType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.nio.file.Path;

public class CsvManagerFs extends FsTextManager {

  private static CsvManagerFs csvManager;

  public static CsvManagerFs getCsvManagerSingleton() {
    if (csvManager == null) {
      csvManager = new CsvManagerFs();
    }
    return csvManager;
  }

  @Override
  public CsvDataPath createDataPath(FsConnection fsConnection, Path relativePath, MediaType mediaType) {

    return new CsvDataPath(fsConnection, relativePath);

  }

  @Override
  public void create(FsDataPath fsDataPath) {

    CsvDataPath csvDataPath = (CsvDataPath) fsDataPath;
    Fs.createEmptyFile(csvDataPath.getAbsoluteNioPath());
    CSVFormat csvFormat = csvDataPath.getCsvFormat();
    if (csvFormat.getRecordSeparator()==null){
      csvFormat = csvFormat.withRecordSeparator(System.lineSeparator());
    }
    if (csvDataPath.getHeaderRowId() > 0) {
      final String[] headers = csvDataPath.getOrCreateRelationDef().getColumnDefs()
        .stream()
        .map(ColumnDef::getColumnName).toArray(String[]::new);
      if (headers.length != 0) {
        csvFormat = csvFormat.withHeader(headers);
      } else {
        /**
         * The default format has an header {@link CsvDataDefFs#headerRowCount}
         * therefore you can come here when you create an empty file
         */
        throw new IllegalStateException("The CSV file has a format asking to print the headers but there is no columns defined for this CSV");
      }
    }

    // Creation of the file with the header or not
    try {
      CSVPrinter printer = csvFormat
        .print(csvDataPath.getAbsoluteNioPath(), csvDataPath.getCharset());
      printer.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }




}
