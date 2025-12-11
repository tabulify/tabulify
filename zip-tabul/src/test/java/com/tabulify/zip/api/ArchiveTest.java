package com.tabulify.zip.api;

import com.tabulify.fs.Fs;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class ArchiveTest {

  @Test
  void zipTest() throws IOException, URISyntaxException {
    for (ArchiveMediaType expectedMediaType : ArchiveMediaType.values()) {

      Path destinationPath = Files.createTempDirectory("tabulify-zip");
      Path fooTarget = destinationPath.resolve("zip/foo.txt");
      Assertions.assertFalse(Files.exists(fooTarget));
      URL resource = ArchiveTest.class.getResource("/archives/archive." + expectedMediaType.getExtension());
      if (resource == null) {
        throw new RuntimeException("Could not find archive " + expectedMediaType.getExtension());
      }
      Path path = Paths.get(resource.toURI());

      Archive archive = Archive
        .builder()
        .setArchive(path)
        .build();

      ArchiveExtractor
        .builder()
        .setDestinationDirectory(destinationPath)
        .setGlobNameSelector("*foo.txt")
        .build()
        .extract(archive);

      Assertions.assertTrue(Files.exists(fooTarget), "The file should have been extracted from the archive " + expectedMediaType.getExtension());

      String fileContent = Fs.getFileContent(fooTarget);
      Assertions.assertEquals("Foo\r\n", fileContent, "The file extracted from " + path.getFileName() + " is not empty");

      long size = Files.size(path);
      Assertions.assertTrue(size > 0, "The file is not empty");

    }


  }


}
