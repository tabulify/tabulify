package net.bytle.db.csv;

import net.bytle.db.fs.FsFileManager;
import net.bytle.db.fs.FsTableSystem;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.spi.DataPath;
import net.bytle.fs.Fs;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class CsvManager extends FsFileManager {

  private static Map<FsTableSystem, CsvManager> csvManagers = new HashMap<>();

  public CsvManager(FsTableSystem fsTableSystem) {
    super(fsTableSystem);
  }

  public void create(DataPath dataPath) {

        CsvDataPath csvDataPath = (CsvDataPath) dataPath;
        Fs.createFile(csvDataPath.getNioPath());
        CsvDataDef csvDataDef = csvDataPath.getDataDef();
        CSVFormat csvFormat = csvDataPath.getDataDef().getCsvFormat();
        if (csvDataDef.getHeaderRowCount() > 0) {
            final String[] headers = csvDataDef.getColumnDefs().stream()
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
  public CsvDataPath getDataPath(Path path) {
    return new CsvDataPath(getFsTableSystem(),path);
  }

  public static FsFileManager of(FsTableSystem fsTableSystem) {
    CsvManager csvManager = csvManagers.get(fsTableSystem);
    if (csvManager == null){
      csvManager = new CsvManager(fsTableSystem);
      csvManagers.put(fsTableSystem,csvManager);
    }
    return csvManager;
  }

}
