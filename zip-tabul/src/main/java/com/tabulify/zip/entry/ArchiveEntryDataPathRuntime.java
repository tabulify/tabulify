package com.tabulify.zip.entry;

import com.tabulify.fs.FsConnection;
import com.tabulify.fs.FsDataPath;
import com.tabulify.fs.binary.FsBinaryDataPath;
import com.tabulify.spi.DataPath;
import com.tabulify.zip.api.Archive;
import com.tabulify.zip.api.ArchiveIterator;
import com.tabulify.glob.Glob;
import com.tabulify.type.KeyNormalizer;
import com.tabulify.type.MediaType;
import com.tabulify.type.MediaTypeAbs;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ArchiveEntryDataPathRuntime extends FsBinaryDataPath {

  /**
   * Java Media type
   */
  final static public MediaType ARCHIVE_ENTRY_RUNTIME = new MediaTypeAbs() {
    @Override
    public String getSubType() {
      return "vnd.tabulify." + ArchiveEntryManifestProvider.ARCHIVE_ENTRY_MANIFEST.getKind() + ".runtime";
    }

    @Override
    public String getType() {
      return "application";
    }

    @Override
    public KeyNormalizer getKind() {
      return KeyNormalizer.createSafe(ArchiveEntryManifestProvider.ARCHIVE_ENTRY_MANIFEST.getKind() + "-runtime");
    }

    @Override
    public boolean isRuntime() {
      return true;
    }

  };
  private final ArchiveEntryDataPath archiveEntry;
  private FsDataPath destinationDataPath;

  public ArchiveEntryDataPathRuntime(FsConnection executionConnection, ArchiveEntryDataPath dataPath) {
    super(executionConnection, Paths.get("/").relativize(Paths.get(dataPath.getAbsoluteNioPath().toString())), ARCHIVE_ENTRY_RUNTIME);
    this.archiveEntry = dataPath;
  }

  @Override
  public DataPath execute() {

    if (this.destinationDataPath != null) {
      return this.destinationDataPath;
    }

    Path path = this.archiveEntry.getAbsoluteNioPath();
    Archive archive = Archive.builder()
      .setArchive(path)
      .build();
    try (ArchiveIterator archiveIterator = ArchiveIterator
      .builder()
      .setArchive(archive)
      .setNameSelector(Glob.createOf(this.archiveEntry.getEntryPath()))
      .build()
    ) {
      if (archiveIterator.hasNext()) {
        /**
         * Get the destination
         */
        destinationDataPath = (FsDataPath) this.getConnection().getDataPath(this.archiveEntry.getEntryPath());
        /**
         * Move the pointer to the next archive entry
         */
        archiveIterator.next();
        /**
         * Copy into the path
         */
        archiveIterator.copyCurrentEntryToPath(destinationDataPath.getAbsoluteNioPath());
        /**
         * Return
         */
        return destinationDataPath;
      }
    }
    throw new RuntimeException("The archive entry (" + this.archiveEntry.getEntryPath() + ") was not found in the archive (" + archive + ")");

  }


}
