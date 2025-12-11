package com.tabulify.zip.api;

import com.tabulify.exception.CastException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

class ArchiveTypeDetectorTest {

  @Test
  void zipTest() throws CastException {

    for (ArchiveMediaType expectedMediaType : ArchiveMediaType.values()) {

      Path path = Paths.get("/archives/archive." + expectedMediaType.getExtension());
      ArchiveMediaType archiveMediaType = ArchiveMediaType.cast(path);

      Assertions.assertEquals(expectedMediaType, archiveMediaType);
    }


  }


}
