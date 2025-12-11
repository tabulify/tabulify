package com.tabulify.zip.api;

import com.tabulify.exception.CastException;
import com.tabulify.fs.Fs;
import com.tabulify.glob.Glob;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;

public class ArchiveIterator implements Iterator<ArchiveEntry>, AutoCloseable {

  private final Archive archive;
  private final InputStream fileInputStream;
  private final BufferedInputStream bufferedInputStream;
  private final ArchiveInputStream<?> archiveInputStream;
  private final Glob nameSelectorGlob;
  private final ArchiveIteratorBuilder builder;

  private ArchiveEntry actualElement;


  public ArchiveIterator(ArchiveIteratorBuilder archiveIteratorBuilder) {
    this.archive = archiveIteratorBuilder.archive;
    this.nameSelectorGlob = archiveIteratorBuilder.nameSelectorGlob;
    this.builder = archiveIteratorBuilder;
    try {
      this.fileInputStream = Files.newInputStream(archive.archivePath);
      this.bufferedInputStream = new BufferedInputStream(fileInputStream);
      this.archiveInputStream = createArchiveInputStream(bufferedInputStream, this.archive.archivePath);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static ArchiveIteratorBuilder builder() {
    return new ArchiveIteratorBuilder();
  }


  @Override
  public boolean hasNext() {

    actualElement = null;
    try {

      org.apache.commons.compress.archivers.ArchiveEntry commonArchiveEntry;
      while ((commonArchiveEntry = this.archiveInputStream.getNextEntry()) != null) {

        if (!archiveInputStream.canReadEntryData(commonArchiveEntry)) {
          // log something?
          throw new RuntimeException("Could not read archive entry " + commonArchiveEntry);
        }

        /**
         * An archive entry wrapper
         */
        ArchiveEntry entry = new ArchiveEntry(commonArchiveEntry);

        /**
         * Directory
         */
        if (entry.isDirectory() && this.builder.skipDirectory) {
          continue;
        }

        /**
         * Matched group
         */
        if (this.nameSelectorGlob != null) {
          Matcher matcher = this.nameSelectorGlob.toMatcher(entry.getName());
          if (!matcher.find()) {
            continue;
          }
          List<String> groups = new ArrayList<>();
          groups.add(entry.getName());
          for (int i = 1; i <= matcher.groupCount(); i++) {
            groups.add(matcher.group(i));
          }
          entry.setMatchedGroups(groups);
        }

        actualElement = entry;
        return true;

      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return false;

  }

  @Override
  public ArchiveEntry next() {
    if (actualElement == null) {
      throw new NoSuchElementException();
    }
    return actualElement;
  }

  @Override
  public void close() {
    try {
      this.archiveInputStream.close();
      this.bufferedInputStream.close();
      this.fileInputStream.close();
      this.isClosed = true;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static ArchiveInputStream<?> createArchiveInputStream(BufferedInputStream inputStream, Path path) throws IOException {
    ArchiveMediaType format;
    try {
      format = ArchiveMediaType.cast(path);
    } catch (CastException e) {
      throw new IllegalArgumentException("The archive file type (" + path + ") is not recognized or not supported. Error: " + e.getMessage(), e);
    }
    switch (format) {
      case ZIP:
        return new ZipArchiveInputStream(inputStream);
      case TAR:
        return new TarArchiveInputStream(inputStream);
      case TAR_GZ:
        return new TarArchiveInputStream(new GzipCompressorInputStream(inputStream));
      default:
        throw new IllegalArgumentException("Unsupported archive format: " + format);
    }
  }


  public Path extractActualEntry() {

    if (actualElement == null) {
      throw new NoSuchElementException("There is no current entry. use next() first.");
    }

    String entryName = actualElement.getName();
    if (builder.destinationDirectory == null) {
      throw new IllegalArgumentException("The destination directory is required to extract an entry or use the copyActualEntryToPath");
    }
    Path outputPath = builder.destinationDirectory.resolve(entryName);

    copyCurrentEntryToPath(outputPath);
    return outputPath;

  }

  /**
   * @param outputPath - write the current archive entry to this path
   */
  public void copyCurrentEntryToPath(Path outputPath) {

    // Create parent directories if needed
    Path targetDirectory = outputPath.getParent();
    Fs.createDirectoryIfNotExists(targetDirectory);
    try (OutputStream outputStream = Files.newOutputStream(
      outputPath,
      StandardOpenOption.CREATE,
      StandardOpenOption.TRUNCATE_EXISTING
    )) {
      int count = IOUtils.copy(this.archiveInputStream, outputStream);
    } catch (IOException e) {
      Fs.validateDirectoryPath(targetDirectory);
      throw new RuntimeException("Write Error. We couldn't extract the current entry (" + actualElement.getName() + ") of the archive " + archive + " into the path " + outputPath + ". Error: " + e.getMessage(), e);
    }

  }

  private boolean isClosed = false;


  public boolean isClosed() {
    return this.isClosed;
  }


  public static class ArchiveIteratorBuilder {
    private Archive archive;
    private Glob nameSelectorGlob;
    private Path destinationDirectory;
    private boolean skipDirectory = false;

    public ArchiveIteratorBuilder setArchive(Archive archive) {
      this.archive = archive;
      return this;
    }

    public ArchiveIteratorBuilder setNameSelector(Glob nameSelectorGlob) {
      this.nameSelectorGlob = nameSelectorGlob;
      return this;
    }

    public ArchiveIteratorBuilder setDestinationFolder(Path destinationDirectory) {
      this.destinationDirectory = destinationDirectory;
      return this;
    }

    public ArchiveIterator build() {
      return new ArchiveIterator(this);
    }

    public ArchiveIteratorBuilder setSkipDirectory(boolean skipDirectory) {
      this.skipDirectory = skipDirectory;
      return this;
    }
  }
}
