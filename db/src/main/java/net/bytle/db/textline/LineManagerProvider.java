package net.bytle.db.textline;

import net.bytle.db.fs.FsFileManager;
import net.bytle.db.fs.FsFileManagerProvider;

import java.nio.file.Path;

public class LineManagerProvider extends FsFileManagerProvider {
  private LineManager lineManager;

  @Override
  public Boolean accept(Path path) {

    if (path.toString().toLowerCase().endsWith("txt")){
      return true;
    } else {
      return false;
    }
  }

  @Override
  public FsFileManager getFsFileManager() {
    if (lineManager == null){
      lineManager = new LineManager();
    }
    return lineManager;
  }
}
