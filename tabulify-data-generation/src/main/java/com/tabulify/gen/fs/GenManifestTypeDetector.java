package com.tabulify.gen.fs;

import com.tabulify.gen.GeneratorMediaType;

import java.nio.file.Path;
import java.nio.file.spi.FileTypeDetector;

public class GenManifestTypeDetector extends FileTypeDetector {

  @Override
  public String probeContentType(Path path) {

    if (!path.getFileName().toString().endsWith(GeneratorMediaType.DATA_GEN.getExtension())) {
      return null;
    }
    return GeneratorMediaType.DATA_GEN.toString();

  }

}
