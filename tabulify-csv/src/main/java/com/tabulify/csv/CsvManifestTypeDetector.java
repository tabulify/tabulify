package com.tabulify.csv;


import java.nio.file.Path;
import java.nio.file.spi.FileTypeDetector;

import static com.tabulify.csv.CsvManifest.MANIFEST_MEDIA_TYPE;

public class CsvManifestTypeDetector extends FileTypeDetector {

  @Override
  public String probeContentType(Path path) {

    if (!path.getFileName().toString().endsWith(MANIFEST_MEDIA_TYPE.getExtension())) {
      return null;
    }
    return MANIFEST_MEDIA_TYPE.toString();

  }

}
