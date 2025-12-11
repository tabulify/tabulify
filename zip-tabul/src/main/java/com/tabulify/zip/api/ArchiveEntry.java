package com.tabulify.zip.api;

import com.tabulify.exception.InternalException;
import com.tabulify.type.MediaTypes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ArchiveEntry implements org.apache.commons.compress.archivers.ArchiveEntry {

  private final org.apache.commons.compress.archivers.ArchiveEntry entry;
  /**
   * The matched group during iteration
   */
  private List<String> matchedGroups = new ArrayList<>();

  public ArchiveEntry(org.apache.commons.compress.archivers.ArchiveEntry entry) {
    this.entry = entry;
  }

  @Override
  public Date getLastModifiedDate() {
    return entry.getLastModifiedDate();
  }

  @Override
  public String getName() {
    return entry.getName();
  }

  @Override
  public long getSize() {
    return entry.getSize();
  }

  @Override
  public boolean isDirectory() {
    return entry.isDirectory();
  }

  public Object getValueFromAttribute(ArchiveEntryAttribute name) {
    switch (name) {
      case PATH:
        return entry.getName();
      case SIZE:
        return entry.getSize();
      case UPDATE_TIME:
        return Timestamp.from(entry.getLastModifiedDate().toInstant());
      case MEDIA_TYPE:
        if (entry.isDirectory()) {
          return MediaTypes.DIR;
        }
        String mediaType;
        try {
          mediaType = Files.probeContentType(Paths.get(entry.getName()));
        } catch (IOException e) {
          return "";
        }
        if (mediaType == null) {
          return "";
        }
        return mediaType;
      default:
        throw new InternalException("The archive entry property (" + name + ") is not in the switch branch");
    }
  }

  public List<String> getMatchedGroups() {
    return this.matchedGroups;
  }

  public ArchiveEntry setMatchedGroups(List<String> matchedGroups) {
    this.matchedGroups = matchedGroups;
    return this;
  }
}
