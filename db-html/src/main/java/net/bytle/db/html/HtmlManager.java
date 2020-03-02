package net.bytle.db.html;

import net.bytle.db.fs.FsDataPath;
import net.bytle.db.fs.FsDataStore;
import net.bytle.db.fs.FsBinaryFileManager;

import java.nio.file.Path;

public class HtmlManager extends FsBinaryFileManager {


  @Override
  public FsDataPath createDataPath(FsDataStore fsDataStore, Path path) {
    return new HtmlDataPath(fsDataStore, path);
  }


}
