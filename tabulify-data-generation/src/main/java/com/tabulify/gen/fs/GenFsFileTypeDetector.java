package com.tabulify.gen.fs;

import com.tabulify.gen.GenDataPathType;

import java.nio.file.Path;
import java.nio.file.spi.FileTypeDetector;

public class GenFsFileTypeDetector extends FileTypeDetector {

  @Override
  public String probeContentType(Path path) {

    if (!path.getFileName().toString().endsWith(GenDataPathType.DATA_GEN.getExtension())) {
      return null;
    }
    return GenDataPathType.DATA_GEN.toString();

  }

}
