package com.tabulify.yaml;

import com.tabulify.fs.FsConnection;
import com.tabulify.fs.textfile.FsTextManager;

import java.nio.file.Path;

public class YamlManagerFs extends FsTextManager {


  @Override
  public YamlDataPath createDataPath(FsConnection fsConnection, Path path) {

    return new YamlDataPath(fsConnection, path);

  }


}
