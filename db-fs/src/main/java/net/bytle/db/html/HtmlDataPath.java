package net.bytle.db.html;

import net.bytle.db.fs.struct.FsDataPath;
import net.bytle.db.fs.FsTableSystem;

import java.nio.file.Path;

public class HtmlDataPath extends FsDataPath {

  public HtmlDataPath(FsTableSystem fsTableSystem, Path path) {
    super(fsTableSystem, path);
  }

}
