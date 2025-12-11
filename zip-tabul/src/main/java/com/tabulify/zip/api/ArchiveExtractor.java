package com.tabulify.zip.api;

import com.tabulify.fs.Fs;
import com.tabulify.glob.Glob;
import org.apache.commons.compress.archivers.ArchiveEntry;

import java.io.IOException;
import java.nio.file.Path;


public class ArchiveExtractor {


  private final Builder builder;

  public ArchiveExtractor(Builder builder) {
    this.builder = builder;
  }

  public static ArchiveExtractor.Builder builder() {
    return new ArchiveExtractor.Builder();
  }


  public ArchiveExtractor extract(Archive archive) throws IOException {


    try (ArchiveIterator archiveIterator = ArchiveIterator.builder()
      .setArchive(archive)
      .setNameSelector(this.builder.nameSelectorGlob)
      .setDestinationFolder(this.builder.destinationDirectory)
      .build()
    ) {

      while (archiveIterator.hasNext()) {
        ArchiveEntry entry = archiveIterator.next();
        String entryName = entry.getName();

        Path outputPath = archiveIterator.extractActualEntry();

        System.out.println("Extracted: " + entryName + " (size:" + entry.getSize() + ") to " + outputPath + " from " + archive.archivePath.getFileName().toString());
      }
    }
    return this;

  }

  public static class Builder {

    private Path destinationDirectory;

    private Glob nameSelectorGlob;


    public Builder setDestinationDirectory(Path destinationDirectory) {
      this.destinationDirectory = destinationDirectory;
      return this;
    }

    public Builder setGlobNameSelector(Glob nameSelector) {
      this.nameSelectorGlob = nameSelector;
      return this;
    }

    public Builder setGlobNameSelector(String fileName) {
      this.nameSelectorGlob = Glob.createOf(fileName);
      return this;
    }

    public ArchiveExtractor build() {
      // Create destination directory if it doesn't exist
      Fs.createDirectoryIfNotExists(this.destinationDirectory);
      return new ArchiveExtractor(this);
    }

    public Glob getNameSelector() {
      return this.nameSelectorGlob;
    }
  }


}
