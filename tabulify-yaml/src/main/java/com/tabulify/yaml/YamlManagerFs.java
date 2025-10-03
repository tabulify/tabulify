package com.tabulify.yaml;

import com.tabulify.fs.FsConnection;
import com.tabulify.fs.textfile.FsTextManager;
import net.bytle.type.MediaType;

import java.nio.file.Path;

public class YamlManagerFs extends FsTextManager {


  @Override
  public YamlDataPath createDataPath(FsConnection fsConnection, Path relativePath, MediaType mediaType) {

    return new YamlDataPath(fsConnection, relativePath);

  }


}
