package net.bytle.db.html;

import net.bytle.db.fs.FsFileManagerProvider;
import net.bytle.db.fs.binary.FsBinaryFileManager;
import net.bytle.type.MediaType;

import java.nio.file.Path;

public class HtmlManagerProvider extends FsFileManagerProvider  {

  static private HtmlManager htmlManager;

  @Override
  public boolean accept(Path path) {

    return testEnd(path.getFileName().toString());
  }

  @Override
  public Boolean accept(MediaType mediaType) {
    return this.testEnd(mediaType.toString());
  }

  private Boolean testEnd(String pathOrMediaType) {
    return pathOrMediaType.toLowerCase().endsWith("html") || pathOrMediaType.toLowerCase().endsWith("htm");
  }


  @Override
  public FsBinaryFileManager getFsFileManager() {
    if (htmlManager == null) {
      htmlManager = new HtmlManager();
    }
    return htmlManager;
  }

}
