package com.tabulify.zip.api;

import net.bytle.exception.CastException;

import java.nio.file.Path;
import java.util.Objects;


public class Archive implements Comparable<Archive> {



  public static class Builder {
    private Path archivePath;

    private ArchiveMediaType mediaType;

    public Builder setArchive(Path archive) {
      this.archivePath = archive;
      return this;
    }

    public Builder setMediaType(ArchiveMediaType mediaType) {
      this.mediaType = mediaType;
      return this;
    }


    public Archive build() {
      if (archivePath == null) {
        throw new IllegalArgumentException("Archive file path must be specified");
      }
      if (mediaType == null) {
        try {
          this.mediaType = ArchiveMediaType.cast(archivePath);
        } catch (CastException e) {
          throw new IllegalArgumentException(e);
        }
      }

      return new Archive(this);
    }
  }

  protected final Path archivePath;

  private Archive(Builder builder) {
    this.archivePath = builder.archivePath;

  }


  public static Builder builder() {
    return new Builder();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Archive archive = (Archive) o;
    return Objects.equals(archivePath, archive.archivePath);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(archivePath);
  }

  @Override
  public String toString() {
    return archivePath.toString();
  }

  @Override
  public int compareTo(Archive o) {
    return this.archivePath.compareTo(o.archivePath);
  }

}
