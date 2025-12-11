package com.tabulify.zip.api;


import java.nio.file.Path;
import java.nio.file.spi.FileTypeDetector;


public class ArchiveTypeDetector extends FileTypeDetector {

  @Override
  public String probeContentType(Path path) {


    String lowerName = path.getFileName().toString().toLowerCase();
    for (ArchiveMediaType a : ArchiveMediaType.values()) {
      for (String extension : a.getExtensions()) {
        if (lowerName.endsWith(extension)) {
          return a.toString();
        }
      }
    }
    return null;


  }

}
