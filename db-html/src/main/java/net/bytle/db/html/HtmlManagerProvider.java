package net.bytle.db.html;

import net.bytle.db.fs.FsBinaryFileManager;
import net.bytle.db.fs.FsFileManagerProvider;

import java.nio.file.Path;

public class HtmlManagerProvider extends FsFileManagerProvider {

  static private HtmlManager htmlManager;

  @Override
  public Boolean accept(Path path) {

    if (path.toString().toLowerCase().endsWith("html") || path.toString().toLowerCase().endsWith("htm")) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public FsBinaryFileManager getFsFileManager() {
    if (htmlManager == null) {
      htmlManager = new HtmlManager();
    }
    return htmlManager;
  }

}
