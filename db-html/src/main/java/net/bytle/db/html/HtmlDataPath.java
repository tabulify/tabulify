package net.bytle.db.html;

import net.bytle.db.fs.FsBinaryDataPath;
import net.bytle.db.fs.FsDataStore;

import java.nio.file.Path;

public class HtmlDataPath extends FsBinaryDataPath {

  public HtmlDataPath(FsDataStore fsDataStore, Path path) {
    super(fsDataStore, path);
  }

}
