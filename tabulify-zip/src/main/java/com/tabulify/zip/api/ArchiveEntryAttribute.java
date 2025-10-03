package com.tabulify.zip.api;

import net.bytle.type.KeyInterface;

import java.sql.Timestamp;

/**
 * The property of an entry in the archive
 */
public enum ArchiveEntryAttribute implements KeyInterface {

  PATH(String.class),
  MEDIA_TYPE(String.class),
  SIZE(Long.class),
  UPDATE_TIME(Timestamp.class),
  ;

  private final Class<?> clazz;

  ArchiveEntryAttribute(Class<?> timestampClass) {
    this.clazz = timestampClass;
  }

  public Class<?> getClazz() {
    return this.clazz;
  }

}
