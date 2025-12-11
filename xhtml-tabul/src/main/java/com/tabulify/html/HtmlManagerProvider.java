package com.tabulify.html;

import com.tabulify.fs.FsFileManagerProvider;
import com.tabulify.fs.binary.FsBinaryFileManager;
import com.tabulify.type.MediaType;

import java.nio.file.Path;

public class HtmlManagerProvider extends FsFileManagerProvider  {

  static private HtmlManager htmlManager;


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
