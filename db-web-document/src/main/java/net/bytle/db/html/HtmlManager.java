package net.bytle.db.html;

import net.bytle.db.fs.FsConnection;
import net.bytle.db.fs.FsDataPath;
import net.bytle.db.fs.binary.FsBinaryFileManager;

import java.nio.file.Path;

public class HtmlManager extends FsBinaryFileManager {


  @Override
  public FsDataPath createDataPath(FsConnection fsConnection, Path path) {
    return new HtmlDataPath(fsConnection, path);
  }


}
