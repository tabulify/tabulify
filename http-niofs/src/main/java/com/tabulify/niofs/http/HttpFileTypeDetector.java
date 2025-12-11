package com.tabulify.niofs.http;

import com.tabulify.fs.Fs;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileTypeDetector;

public class HttpFileTypeDetector extends FileTypeDetector {

  @Override
  public String probeContentType(Path path) throws IOException {

    boolean b = path instanceof HttpPath;
    if (!b) {
      return null;
    }
    HttpPath httpPath = (HttpPath) path;

    /**
     * We try to get the content type by path extension first
     * Why?
     * * Some website does not return the content type. Example: https://pkgstore.datahub.io/core/s-and-p-500-companies/constituents/archive/652de3c89c39dafdee912fd9cfb23c21/constituents.csv
     * * Some website report a non-correct value due to standard. ie Tar.gz is reported as `application/gzip` (gzip is a content compressor, not an archive)
     * but Tar.gz is an archive type
     * * We economize a round-trip
     */
    String extension = Fs.getExtension(httpPath);
    if (extension != null && (httpPath.getQuery() == null || httpPath.getQuery().isEmpty())) {
      String urlPath = httpPath.getURLPath();
      String contentType = Files.probeContentType(Paths.get(urlPath));
      if (!contentType.equalsIgnoreCase("application/octet-stream")) {
        return contentType;
      }
    }

    /**
     * No content type, no extension
     * Ask the server
     */
    URL url = httpPath.toUri().toURL();
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestProperty(HttpHeader.USER_AGENT.toKeyNormalizer().toHttpHeaderCase(), HttpHeader.USER_AGENT.toString());
    return conn.getContentType();

  }
}
