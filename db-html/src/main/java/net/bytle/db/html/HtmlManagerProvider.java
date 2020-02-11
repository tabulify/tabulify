package net.bytle.db.html;

import net.bytle.db.fs.FsFileManager;
import net.bytle.db.fs.FsFileManagerProvider;

import java.util.Arrays;
import java.util.List;

public class HtmlManagerProvider extends FsFileManagerProvider {

  static private HtmlManager htmlManager;

  @Override
  public List<String> getContentType() {
    return Arrays.asList("html","htm");
  }

  @Override
  public FsFileManager getFsFileManager() {
    if (htmlManager == null){
      htmlManager = new HtmlManager();
    }
    return htmlManager;
  }

}
