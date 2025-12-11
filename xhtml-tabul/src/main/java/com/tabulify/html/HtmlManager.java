package com.tabulify.html;

import com.tabulify.fs.FsConnection;
import com.tabulify.fs.FsDataPath;
import com.tabulify.fs.binary.FsBinaryFileManager;
import com.tabulify.type.MediaType;

import java.nio.file.Path;

public class HtmlManager extends FsBinaryFileManager {


  @Override
  public FsDataPath createDataPath(FsConnection fsConnection, Path relativePath, MediaType mediaType) {
    return new HtmlDataPath(fsConnection, relativePath);
  }


}
