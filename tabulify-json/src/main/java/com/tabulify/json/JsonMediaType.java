package com.tabulify.json;

import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.type.Casts;
import net.bytle.type.Enums;
import net.bytle.type.MediaType;

public enum JsonMediaType implements MediaType {

  JSON,
  JSONL;

  public static JsonMediaType cast(MediaType mediaType) throws CastException {

    String type = mediaType.getType();
    if (type != null && !type.equalsIgnoreCase("text")) {
      throw new CastException("Json is a text file, this is a " + type);
    }
    try {
      return Casts.cast(mediaType.getSubType(), JsonMediaType.class);
    } catch (CastException e) {
      throw new CastException("The media type " + mediaType + " is not a json type. We were expecting one of " + Enums.toConstantAsStringCommaSeparated(JsonMediaType.class), e);
    }

  }

  /**
   * Used in constructor to return an enum so that we can use
   * any equality
   */
  public static MediaType castSafe(MediaType mediaType) {
    try {
      return cast(mediaType);
    } catch (CastException e) {
      throw new InternalException("This media type was accepted by the JsonProvider, it should be good", e);
    }
  }

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
}
