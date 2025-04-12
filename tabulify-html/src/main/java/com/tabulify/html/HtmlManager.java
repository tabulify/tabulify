package com.tabulify.html;

import com.tabulify.fs.FsConnection;
import com.tabulify.fs.FsDataPath;
import com.tabulify.fs.binary.FsBinaryFileManager;

import java.nio.file.Path;

public class HtmlManager extends FsBinaryFileManager {


  @Override
  public FsDataPath createDataPath(FsConnection fsConnection, Path path) {
    return new HtmlDataPath(fsConnection, path);
  }


}
