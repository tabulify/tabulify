package com.tabulify.zip.api;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

class ArchiveIteratorTest {

  @Test
  void archiveIterator() throws URISyntaxException {
    for (ArchiveMediaType expectedMediaType : ArchiveMediaType.values()) {
      URL resource = ArchiveTest.class.getResource("/archives/archive." + expectedMediaType.getExtension());
      if (resource == null) {
        throw new RuntimeException("Could not find archive " + expectedMediaType.getExtension());
      }
      Path path = Paths.get(resource.toURI());
      Archive archive = Archive
        .builder()
        .setArchive(path)
        .build();

      System.out.println("Iterating " + path.getFileName());
      try (ArchiveIterator iterator = ArchiveIterator.builder()
        .setArchive(archive)
        .build()) {
        while (iterator.hasNext()) {
          ArchiveEntry archiveEntry = iterator.next();
          System.out.println(archiveEntry.getName() + " (size: " + archiveEntry.getSize() + ")");
        }
      }
      System.out.println(" ");
    }
  }

}
