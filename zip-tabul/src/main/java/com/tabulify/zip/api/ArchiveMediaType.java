package com.tabulify.zip.api;

import com.tabulify.exception.CastException;
import com.tabulify.type.MediaType;
import com.tabulify.type.MediaTypes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum ArchiveMediaType implements MediaType {

  ZIP("application", "zip", List.of("zip")),
  /**
   * <a href="https://superuser.com/questions/901962/what-is-the-correct-mime-type-for-a-tar-gz-file">...</a>
   */
  TAR_GZ("application", "x-tar+gzip", List.of("tar.gz", "tgz")),
  /**
   * Without the {@link ArchiveTypeDetector}, {@link Files#probeContentType(Path)}
   * would have returned `application/gzip` for a `tar.gz` file
   * but gzip is not an archive format. It can only compress individual file.
   */
  // GZIP("application", "gzip", List.of("gz")),
  TAR("application", "x-tar", List.of("tar"));

  private final String type;
  private final String subtype;
  private final List<String> extensions;

  ArchiveMediaType(String type, String subtype, List<String> extensions) {
    this.type = type;
    this.subtype = subtype;
    this.extensions = extensions;
  }

  public static ArchiveMediaType cast(Path archivePath) throws CastException {
    String mediaType;
    try {
      mediaType = Files.probeContentType(archivePath);
    } catch (IOException e) {
      throw new CastException("Error while probing the type of " + archivePath + ". Error: " + e.getMessage(), e);
    }
    for (ArchiveMediaType type : values()) {
      if (type.toString().toLowerCase().equals(mediaType)) {
        return type;
      }
    }
    throw new CastException("The media type (" + mediaType + ") is not a supported archive format. Possible values: " + Arrays.stream(values()).map(ArchiveMediaType::toString).collect(Collectors.joining(", ")) + ". File: " + archivePath);


  }

  public static ArchiveMediaType castSafe(MediaType mediaType) {
    try {
      return cast(mediaType);
    } catch (CastException e) {
      throw new RuntimeException(e);
    }
  }

  public static ArchiveMediaType cast(MediaType mediaType) throws CastException {
    for (ArchiveMediaType type : values()) {
      if (MediaTypes.equals(type, mediaType)) {
        return type;
      }
    }
    throw new CastException("The media type (" + mediaType + ") is not a supported archive format. Possible values: " + Arrays.stream(values()).map(ArchiveMediaType::toString).collect(Collectors.joining(", ")));
  }

  @Override
  public String getSubType() {
    return this.subtype;
  }

  @Override
  public String getType() {
    return this.type;
  }

  @Override
  public boolean isContainer() {
    return false;
  }

  @Override
  public String getExtension() {
    return this.extensions.get(0);
  }

  public List<String> getExtensions() {
    return this.extensions;
  }

  @Override
  public String toString() {
    return this.type + "/" + this.subtype;
  }

}
