package net.bytle.db.csv;

import net.bytle.db.fs.FsBinaryFileManager;
import net.bytle.db.fs.FsFileManagerProvider;

import java.nio.file.Path;

public class CsvManagerProvider extends FsFileManagerProvider {
  private CsvManager csvManager;

  @Override
  public Boolean accept(Path path) {

    if (path.toString().toLowerCase().endsWith("csv")){
      return true;
    } else {
      return false;
    }
  }

  @Override
  public FsBinaryFileManager getFsFileManager() {
    if (csvManager == null){
      csvManager = new CsvManager();
    }
    return csvManager;
  }
}
