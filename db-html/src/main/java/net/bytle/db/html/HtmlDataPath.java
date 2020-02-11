package net.bytle.db.html;

import net.bytle.db.fs.FsDataPath;
import net.bytle.db.fs.FsDataStore;

import java.nio.file.Path;

public class HtmlDataPath extends FsDataPath {

  public HtmlDataPath(FsDataStore fsDataStore, Path path) {
    super(fsDataStore, path);
  }

}
