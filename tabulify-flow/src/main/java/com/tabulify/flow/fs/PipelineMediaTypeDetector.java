package com.tabulify.flow.fs;

import java.nio.file.Path;
import java.nio.file.spi.FileTypeDetector;

public class PipelineMediaTypeDetector extends FileTypeDetector {

  @Override
  public String probeContentType(Path path) {

    if (!path.getFileName().toString().endsWith(PipelineMediaType.PIPELINE.getExtension())) {
      return null;
    }
    return PipelineMediaType.PIPELINE.toString();

  }

}
