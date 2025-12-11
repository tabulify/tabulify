package com.tabulify.gen.fs;

import com.tabulify.gen.GeneratorMediaType;

import java.nio.file.Path;
import java.nio.file.spi.FileTypeDetector;

public class GenManifestTypeDetector extends FileTypeDetector {

  @Override
  public String probeContentType(Path path) {

    if (!path.getFileName().toString().endsWith(GeneratorMediaType.FS_GENERATOR_TYPE.getExtension())) {
      return null;
    }
    return GeneratorMediaType.FS_GENERATOR_TYPE.toString();

  }

}
