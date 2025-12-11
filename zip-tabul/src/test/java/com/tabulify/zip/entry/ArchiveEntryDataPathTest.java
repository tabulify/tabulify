package com.tabulify.zip.entry;

import com.tabulify.Tabular;
import com.tabulify.fs.FsDataPath;
import com.tabulify.spi.DataPath;
import com.tabulify.type.MediaTypes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static com.tabulify.zip.entry.ArchiveEntryDataPathRuntime.ARCHIVE_ENTRY_RUNTIME;
import static com.tabulify.zip.entry.ArchiveEntryManifestProvider.ARCHIVE_ENTRY_MANIFEST;

class ArchiveEntryDataPathTest {

  @Test
  void baseline() throws URISyntaxException {
    try (Tabular tabular = Tabular.tabularWithoutConfigurationFile()) {

      String extension = ARCHIVE_ENTRY_MANIFEST.getExtension();
      Path path = Paths.get(Objects.requireNonNull(ArchiveEntryDataPathTest.class
          .getResource("/manifest/world-sql" + extension))
        .toURI());
      DataPath actual = tabular.getDataPath(path);
      Assertions.assertEquals(ArchiveEntryDataPath.class, actual.getClass());
      ArchiveEntryDataPath archiveEntryDataPath = (ArchiveEntryDataPath) actual;
      Assertions.assertEquals("world-db/world.sql", archiveEntryDataPath.getEntryPath());


      DataPath executableDataPath = tabular.getResourceDataPath(ArchiveEntryDataPathTest.class, "manifest/world-sql" + extension);
      ArchiveEntryDataPathRuntime runtimeEntry = (ArchiveEntryDataPathRuntime) tabular.getTmpConnection().getRuntimeDataPath (executableDataPath, null);
      Assertions.assertEquals("world-db.tar.gz", runtimeEntry.getNioPath().getFileName().toString());
      Assertions.assertTrue(MediaTypes.equals(ARCHIVE_ENTRY_RUNTIME, runtimeEntry.getMediaType()));
      Assertions.assertTrue(runtimeEntry.isRuntime());

      FsDataPath runtimeEntryPath = (FsDataPath) runtimeEntry.execute();
      Assertions.assertEquals("world.sql", runtimeEntryPath.getNioPath().getFileName().toString());
      Assertions.assertEquals(MediaTypes.TEXT_SQL.toString(), runtimeEntryPath.getMediaType().toString());
      /**
       * Test a loop
       */
      Assertions.assertEquals(5382, runtimeEntryPath.getCount());

    }
  }
}
