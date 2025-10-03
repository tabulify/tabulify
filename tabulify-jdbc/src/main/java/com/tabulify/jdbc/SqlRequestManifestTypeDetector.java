package com.tabulify.jdbc;


import java.nio.file.Path;
import java.nio.file.spi.FileTypeDetector;

public class SqlRequestManifestTypeDetector extends FileTypeDetector {

  @Override
  public String probeContentType(Path path) {

    if (!path.getFileName().toString().endsWith(SqlRequestManifest.SQL_REQUEST_YAML.getExtension())) {
      return null;
    }
    return SqlRequestManifest.SQL_REQUEST_YAML.toString();

  }

}
