package com.tabulify.resource;


import java.nio.file.Path;
import java.nio.file.spi.FileTypeDetector;

public class ManifestResourceFileTypeDetector extends FileTypeDetector {

  @Override
  public String probeContentType(Path path) {

    if (!path.getFileName().toString().endsWith(ManifestResourceProvider.MEDIA_TYPE.getExtension())) {
      return null;
    }
    return ManifestResourceProvider.MEDIA_TYPE.toString();

  }

}
