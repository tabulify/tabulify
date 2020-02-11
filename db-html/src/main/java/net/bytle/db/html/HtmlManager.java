package net.bytle.db.html;

import net.bytle.db.fs.FsDataPath;
import net.bytle.db.fs.FsDataStore;
import net.bytle.db.fs.FsFileManager;

import java.nio.file.Path;

public class HtmlManager extends FsFileManager {


  @Override
  public FsDataPath createDataPath(FsDataStore fsDataStore, Path path) {
    return new HtmlDataPath(fsDataStore, path);
  }


}
