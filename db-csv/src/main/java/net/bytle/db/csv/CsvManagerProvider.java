package net.bytle.db.csv;

import net.bytle.db.fs.FsFileManager;
import net.bytle.db.fs.FsFileManagerProvider;

import java.util.Arrays;
import java.util.List;

public class CsvManagerProvider extends FsFileManagerProvider {
  private CsvManager csvManager;

  @Override
  public List<String> getContentType() {
    return Arrays.asList("csv");
  }

  @Override
  public FsFileManager getFsFileManager() {
    if (csvManager == null){
      csvManager = new CsvManager();
    }
    return csvManager;
  }
}
