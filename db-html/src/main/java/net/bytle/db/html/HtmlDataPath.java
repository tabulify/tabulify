package net.bytle.db.html;

import net.bytle.db.fs.FsRawDataPath;
import net.bytle.db.fs.FsDataStore;

import java.nio.file.Path;

public class HtmlDataPath extends FsRawDataPath {

  public HtmlDataPath(FsDataStore fsDataStore, Path path) {
    super(fsDataStore, path);
  }

}
