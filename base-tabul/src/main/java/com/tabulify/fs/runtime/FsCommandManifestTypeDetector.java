package com.tabulify.fs.runtime;


import java.nio.file.Path;
import java.nio.file.spi.FileTypeDetector;

import static com.tabulify.fs.runtime.FsCommandManifestProvider.COMMAND_MANIFEST_TYPE;

public class FsCommandManifestTypeDetector extends FileTypeDetector {

  @Override
  public String probeContentType(Path path) {

    if (!path.getFileName().toString().endsWith(COMMAND_MANIFEST_TYPE.getExtension())) {
      return null;
    }
    return COMMAND_MANIFEST_TYPE.toString();

  }

}
