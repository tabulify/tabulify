package com.tabulify.json;

import com.tabulify.fs.FsConnection;
import com.tabulify.fs.textfile.FsTextManager;
import net.bytle.type.MediaType;

import java.nio.file.Path;

public class JsonManagerFs extends FsTextManager {


  @Override
  public JsonDataPath createDataPath(FsConnection fsConnection, Path relativePath, MediaType mediaType) {

    return new JsonDataPath(fsConnection, relativePath, mediaType);

  }


}
