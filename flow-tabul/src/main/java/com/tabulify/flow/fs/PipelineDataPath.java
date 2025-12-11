package com.tabulify.flow.fs;

import com.tabulify.fs.FsConnection;
import com.tabulify.fs.textfile.FsTextDataPath;
import com.tabulify.type.MediaType;

import java.nio.file.Path;

public class PipelineDataPath extends FsTextDataPath {

  public PipelineDataPath(FsConnection fsConnection, Path path, MediaType mediaType) {
    super(fsConnection, path, mediaType);
  }

}
