package com.tabulify.fs.sql;

import com.tabulify.exception.CastException;
import com.tabulify.type.Casts;
import com.tabulify.type.Enums;
import com.tabulify.type.MediaType;

public enum FsSqlMediaType implements MediaType {

  SQL;

  @Override
  public String getSubType() {
    return this.name().toLowerCase();
  }

  @Override
  public String getType() {
    return "text";
  }

  @Override
  public boolean isContainer() {
    return false;
  }

  @Override
  public String getExtension() {
    return this.name().toLowerCase();
  }

  @Override
  public String toString() {
    return getType() + "/" + getSubType();
  }


  public static FsSqlMediaType cast(MediaType mediaType) throws CastException {

    String type = mediaType.getType();
    if (type != null && !type.equalsIgnoreCase("text")) {
      throw new CastException("Sql is a text file, this is a " + type);
    }
    try {
      return Casts.cast(mediaType.getSubType(), FsSqlMediaType.class);
    } catch (CastException e) {
      throw new CastException("The media type " + mediaType + " is not a sql type. We were expecting one of " + Enums.toConstantAsStringCommaSeparated(FsSqlMediaType.class), e);
    }

  }
}
