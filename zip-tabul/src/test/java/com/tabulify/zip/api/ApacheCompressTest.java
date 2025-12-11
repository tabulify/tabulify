package com.tabulify.zip.api;

import com.tabulify.fs.Fs;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ApacheCompressTest {

  /**
   * A basic code of apache compress
   * to check that the library is working
   */
  @Test
  void basic() throws URISyntaxException, IOException {
    for (ArchiveMediaType expectedMediaType : ArchiveMediaType.values()) {
      URL resource = ArchiveTest.class.getResource("/archives/archive." + expectedMediaType.getExtension());
      if (resource == null) {
        throw new RuntimeException("Could not find archive ");
      }
      Path archive = Paths.get(resource.toURI());
      Path targetDir = Files.createTempDirectory("tabulify-zip");
      try (InputStream fileInputStream = Files.newInputStream(archive);
           BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
           ArchiveInputStream<?> archiveInputStream = ArchiveIterator.createArchiveInputStream(bufferedInputStream, archive);
      ) {

        org.apache.commons.compress.archivers.ArchiveEntry entry;
        while ((entry = archiveInputStream.getNextEntry()) != null) {
          if (!archiveInputStream.canReadEntryData(entry)) {
            throw new RuntimeException("Archive entry " + entry.getName() + " is not readable");
          }
          if (entry.isDirectory()) {
            continue;
          }
          Path targetPath = targetDir.resolve(entry.getName());
          Path targetDirectory = targetPath.getParent();
          Fs.createDirectoryIfNotExists(targetDirectory);
          try (OutputStream o = Files.newOutputStream(targetPath)) {
            int count = IOUtils.copy(archiveInputStream, o);
            System.out.println(count + " for file " + entry.getName());
          }

        }

      }

      Path fooFile = targetDir.resolve("zip/foo.txt");
      String fileContent = Fs.getFileContent(fooFile);
      Assertions.assertEquals("Foo\r\n", fileContent, "The file extracted from " + fooFile.getFileName() + " is good");

    }
  }
}
